package com.zhuxiang.service.controller;

import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.service.AlipayService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.RentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝异步通知回调接口。
 * 无需鉴权，支付宝服务器直接调用。
 */
@RestController
@Tag(name = "支付宝回调", description = "支付宝异步支付通知回调")
public class AlipayNotifyController {

    private static final Logger log = LoggerFactory.getLogger(AlipayNotifyController.class);

    private final AlipayService alipayService;
    private final PaymentRecordService paymentRecordService;
    private final RentOrderService rentOrderService;

    public AlipayNotifyController(
            AlipayService alipayService,
            PaymentRecordService paymentRecordService,
            RentOrderService rentOrderService
    ) {
        this.alipayService = alipayService;
        this.paymentRecordService = paymentRecordService;
        this.rentOrderService = rentOrderService;
    }

    @PostMapping("/payments/alipay/notify")
    @Operation(summary = "支付宝异步通知", description = "支付宝服务器异步通知支付结果，验签通过后更新支付记录和订单状态")
    public String handleNotify(HttpServletRequest request) {
        Map<String, String> params = parseParams(request);
        log.info("收到支付宝异步通知 params={}", params);

        AlipayService.AlipayNotifyResult result = alipayService.verifyNotify(params);
        if (result == null) {
            log.warn("支付宝异步通知验签失败或非成功状态");
            return "fail";
        }

        String outTradeNo = result.outTradeNo();
        String tradeNo = result.tradeNo();

        // 通过 outTradeNo(paymentNo) 查找支付记录
        PaymentRecord record = paymentRecordService.lambdaQuery()
                .eq(PaymentRecord::getPaymentNo, outTradeNo)
                .one();
        if (record == null) {
            log.error("支付记录不存在 outTradeNo={}", outTradeNo);
            return "fail";
        }

        // 幂等处理：已成功的直接返回
        if ("success".equals(record.getStatus())) {
            log.info("支付记录已处理，忽略重复通知 outTradeNo={}", outTradeNo);
            return "success";
        }

        // 金额校验：支付宝回调金额 vs 记录金额
        int alipayAmount = new BigDecimal(result.totalAmount())
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        if (alipayAmount != record.getAmount()) {
            log.error("支付宝回调金额不匹配 outTradeNo={} alipayAmount={} recordAmount={}",
                    outTradeNo, alipayAmount, record.getAmount());
            return "fail";
        }

        try {
            rentOrderService.confirmPayment(record.getId(), tradeNo);
            log.info("支付宝异步通知处理成功 outTradeNo={} tradeNo={}", outTradeNo, tradeNo);
            return "success";
        } catch (Exception e) {
            log.error("处理支付宝异步通知失败 outTradeNo={}", outTradeNo, e);
            return "fail";
        }
    }

    private Map<String, String> parseParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            params.put(name, request.getParameter(name));
        }
        return params;
    }
}
