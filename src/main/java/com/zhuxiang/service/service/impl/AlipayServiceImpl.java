package com.zhuxiang.service.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.zhuxiang.service.config.AlipayProperties;
import com.zhuxiang.service.service.AlipayService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class AlipayServiceImpl implements AlipayService {

    private static final Logger log = LoggerFactory.getLogger(AlipayServiceImpl.class);

    private final AlipayProperties props;
    private AlipayClient alipayClient;

    public AlipayServiceImpl(AlipayProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        DefaultAlipayClient client = new DefaultAlipayClient(
                props.getGatewayUrl(),
                props.getAppId(),
                props.getMerchantPrivateKey(),
                props.getFormat(),
                props.getCharset(),
                props.getAlipayPublicKey(),
                props.getSignType()
        );
        client.setConnectTimeout(10000);
        client.setReadTimeout(30000);
        this.alipayClient = client;
    }

    @Override
    public String buildH5PayUrl(String outTradeNo, int totalAmount, String subject) {
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setNotifyUrl(props.getNotifyUrl());

        BigDecimal amount = BigDecimal.valueOf(totalAmount)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Map<String, Object> bizContent = new java.util.LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", amount.toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", "QUICK_WAP_WAY");
        if (props.getReturnUrl() != null && !props.getReturnUrl().isBlank()) {
            bizContent.put("return_url", props.getReturnUrl());
        }
        request.setBizContent(toJson(bizContent));

        try {
            return alipayClient.pageExecute(request, "GET").getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝统一下单失败 outTradeNo={}", outTradeNo, e);
            throw new RuntimeException("支付宝支付下单失败，请稍后重试", e);
        }
    }

    @Override
    public AlipayNotifyResult queryOrder(String outTradeNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\"}");

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                AlipayTradeQueryResponse response = alipayClient.execute(request);
                if (!response.isSuccess()) {
                    log.warn("支付宝查询订单失败 outTradeNo={} subCode={} subMsg={}",
                            outTradeNo, response.getSubCode(), response.getSubMsg());
                    return null;
                }
                if (!"TRADE_SUCCESS".equals(response.getTradeStatus())) {
                    log.info("支付宝查询订单非成功状态 outTradeNo={} tradeStatus={}",
                            outTradeNo, response.getTradeStatus());
                    return null;
                }
                return new AlipayNotifyResult(
                        response.getTradeNo(),
                        response.getOutTradeNo(),
                        response.getTotalAmount(),
                        response.getTradeStatus(),
                        response.getBuyerLogonId()
                );
            } catch (AlipayApiException e) {
                log.warn("支付宝查询异常 第{}次/共{}次 outTradeNo={} err={}",
                        attempt, maxRetries, outTradeNo, e.getMessage());
                if (attempt == maxRetries) {
                    log.error("支付宝查询最终失败 outTradeNo={}", outTradeNo);
                    return null;
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public AlipayNotifyResult verifyNotify(Map<String, String> params) {
        try {
            boolean ok = AlipaySignature.rsaCheckV1(params, props.getAlipayPublicKey(), props.getCharset(), props.getSignType());
            if (!ok) {
                log.warn("支付宝异步通知验签失败 params={}", params);
                return null;
            }

            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                log.info("支付宝通知非成功状态 tradeStatus={} outTradeNo={}", tradeStatus, params.get("out_trade_no"));
                return null;
            }

            return new AlipayNotifyResult(
                    params.get("trade_no"),
                    params.get("out_trade_no"),
                    params.get("total_amount"),
                    tradeStatus,
                    params.get("buyer_logon_id")
            );
        } catch (AlipayApiException e) {
            log.error("支付宝验签异常", e);
            return null;
        }
    }

    @Override
    public AlipayRefundResult refund(String outTradeNo, String refundAmount, String outRequestNo) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        Map<String, Object> bizContent = new java.util.LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("refund_amount", refundAmount);
        bizContent.put("out_request_no", outRequestNo);
        request.setBizContent(toJson(bizContent));

        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.error("支付宝退款失败 outTradeNo={} outRequestNo={} subCode={} subMsg={}",
                        outTradeNo, outRequestNo, response.getSubCode(), response.getSubMsg());
                return null;
            }
            return new AlipayRefundResult(
                    response.getTradeNo(),
                    response.getOutTradeNo(),
                    response.getRefundFee(),
                    outRequestNo
            );
        } catch (AlipayApiException e) {
            log.error("支付宝退款异常 outTradeNo={} outRequestNo={}", outTradeNo, outRequestNo, e);
            return null;
        }
    }

    @Override
    public AlipayRefundResult queryRefund(String outTradeNo, String outRequestNo) {
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        Map<String, Object> bizContent = new java.util.LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("out_request_no", outRequestNo);
        request.setBizContent(toJson(bizContent));

        try {
            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.warn("支付宝退款查询失败 outTradeNo={} outRequestNo={} subCode={} subMsg={}",
                        outTradeNo, outRequestNo, response.getSubCode(), response.getSubMsg());
                return null;
            }
            return new AlipayRefundResult(
                    response.getTradeNo(),
                    response.getOutTradeNo(),
                    response.getRefundAmount(),
                    outRequestNo
            );
        } catch (AlipayApiException e) {
            log.error("支付宝退款查询异常 outTradeNo={} outRequestNo={}", outTradeNo, outRequestNo, e);
            return null;
        }
    }

    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(value).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
