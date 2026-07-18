package com.zhuxiang.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.service.EsignCallbackData;
import com.zhuxiang.service.service.RentOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * e签宝 V3 签署回调通知接口。
 * <p>
 * 回调签名协议（与主动请求不同）：
 * <ul>
 *   <li>请求头：X-Tsign-Open-App-Id / X-Tsign-Open-SIGNATURE / X-Tsign-Open-TIMESTAMP / X-Tsign-Open-SIGNATURE-ALGORITHM</li>
 *   <li>待签内容：timestamp + "\n" + queryString + "\n" + rawBody（UTF-8 字节）</li>
 *   <li>签名算法：HmacSHA256，密钥为 AppSecret</li>
 * </ul>
 * 必须读取原始 Body 字节验签，不能先反序列化 JSON 再序列化。
 */
@RestController
public class EsignCallbackController {

    private static final Logger log = LoggerFactory.getLogger(EsignCallbackController.class);
    private static final long MAX_AGE_MINUTES = 5;
    private static final ConcurrentHashMap<String, Long> PROCESSED_NONCES = new ConcurrentHashMap<>();

    private final EsignV3Properties properties;
    private final RentOrderService rentOrderService;
    private final ObjectMapper objectMapper;

    public EsignCallbackController(EsignV3Properties properties,
                                   RentOrderService rentOrderService,
                                   ObjectMapper objectMapper) {
        this.properties = properties;
        this.rentOrderService = rentOrderService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/esign/callback")
    public ResponseEntity<String> handleCallback(HttpServletRequest request) {
        // ── 0. 读取原始 Body 字节（只读一次，用于验签 + 后续反序列化） ──
        byte[] rawBodyBytes;
        String rawBody;
        try {
            rawBodyBytes = request.getInputStream().readAllBytes();
            rawBody = new String(rawBodyBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("e签宝回调：读取请求体失败", e);
            return ResponseEntity.ok("OK");
        }

        // ── 1. 读取回调专用请求头 ──
        String appId      = request.getHeader("X-Tsign-Open-App-Id");
        String signature  = request.getHeader("X-Tsign-Open-SIGNATURE");
        String timestamp  = request.getHeader("X-Tsign-Open-TIMESTAMP");
        String algorithm  = request.getHeader("X-Tsign-Open-SIGNATURE-ALGORITHM");

        if (!properties.isCredentialsConfigured()) {
            log.warn("e签宝回调：e签宝未配置");
            return ResponseEntity.ok("OK");
        }
        if (signature == null || signature.isBlank() || timestamp == null || timestamp.isBlank()) {
            log.warn("e签宝回调：缺少签名头 SIGNATURE={}, TIMESTAMP={}",
                    signature != null, timestamp != null);
            return ResponseEntity.status(403).body("Forbidden");
        }

        // ── 2. 防重放：时间戳窗口检查 ──
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.warn("e签宝回调：时间戳格式错误 {}", timestamp);
            return ResponseEntity.status(403).body("Forbidden");
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > MAX_AGE_MINUTES * 60 * 1000L) {
            log.warn("e签宝回调：时间戳过期 ts={}, now={}", ts, now);
            return ResponseEntity.ok("OK");
        }

        // ── 3. 验签 ──
        // 待签内容 = timestamp + "\n" + queryString + "\n" + rawBody
        String queryString = request.getQueryString();
        String contentToSign = ts + "\n"
                + (queryString != null ? queryString : "") + "\n"
                + rawBody;

        String expected = hmacSha256(contentToSign, properties.getAppSecret());
        if (!expected.equals(signature)) {
            log.warn("e签宝回调：签名验证失败 signFlowId(从body)={}",
                    extractSignFlowId(rawBody));
            return ResponseEntity.status(403).body("Forbidden");
        }

        // ── 4. 解析回调数据 ──
        Map<String, Object> data;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(rawBodyBytes, Map.class);
            data = parsed;
        } catch (Exception e) {
            log.warn("e签宝回调：JSON解析失败");
            return ResponseEntity.badRequest().body("Invalid JSON");
        }

        String signFlowId = (String) data.get("signFlowId");
        Integer signFlowStatus = data.get("signFlowStatus") instanceof Number
                ? ((Number) data.get("signFlowStatus")).intValue() : null;

        if (signFlowId == null) {
            log.warn("e签宝回调：缺少 signFlowId");
            return ResponseEntity.badRequest().body("Missing signFlowId");
        }

        // ── 5. 防重放：nonce 去重 ──
        String nonce = signFlowId + "@" + ts;
        if (PROCESSED_NONCES.putIfAbsent(nonce, now) != null) {
            log.info("e签宝回调：重复通知，忽略 signFlowId={}", signFlowId);
            return ResponseEntity.ok("OK");
        }
        if (PROCESSED_NONCES.size() > 10_000) {
            long cutoff = now - MAX_AGE_MINUTES * 2 * 60 * 1000L;
            PROCESSED_NONCES.entrySet().removeIf(e -> e.getValue() < cutoff);
        }

        // ── 6. 处理业务 ──
        log.info("e签宝回调验签通过：signFlowId={}, status={}, contractNum={}",
                signFlowId, signFlowStatus, data.get("contractNum"));

        EsignCallbackData callback = new EsignCallbackData();
        callback.setSignFlowId(signFlowId);
        callback.setSignFlowStatus(signFlowStatus);
        callback.setContractNum((String) data.get("contractNum"));
        Object finishTimeObj = data.get("signFlowFinishTime");
        callback.setSignFlowFinishTime(finishTimeObj instanceof Number
                ? ((Number) finishTimeObj).longValue() : null);

        rentOrderService.processEsignCallback(callback);
        return ResponseEntity.ok("OK");
    }

    // ── 验签前快速提取 signFlowId（日志用） ──
    private static String extractSignFlowId(String rawBody) {
        try {
            int idx = rawBody.indexOf("\"signFlowId\"");
            if (idx < 0) return "?";
            int start = rawBody.indexOf('"', idx + 14);
            int end = rawBody.indexOf('"', start + 1);
            return start > 0 && end > start ? rawBody.substring(start + 1, end) : "?";
        } catch (Exception e) {
            return "?";
        }
    }

    // ── HMAC-SHA256 签名 ──
    static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }
}
