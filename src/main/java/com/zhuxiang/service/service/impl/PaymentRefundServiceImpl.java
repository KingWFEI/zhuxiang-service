package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.entity.PaymentRefund;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.mapper.PaymentRefundMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.AlipayService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.PaymentRefundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentRefundServiceImpl implements PaymentRefundService {
    private static final Logger log = LoggerFactory.getLogger(PaymentRefundServiceImpl.class);
    private static final int MAX_ATTEMPTS = 20;

    private final PaymentRefundMapper refundMapper;
    private final RentOrderMapper orderMapper;
    private final PaymentRecordService paymentRecordService;
    private final AlipayService alipayService;
    private final MessageService messageService;

    public PaymentRefundServiceImpl(PaymentRefundMapper refundMapper,
                                    RentOrderMapper orderMapper,
                                    PaymentRecordService paymentRecordService,
                                    AlipayService alipayService,
                                    MessageService messageService) {
        this.refundMapper = refundMapper;
        this.orderMapper = orderMapper;
        this.paymentRecordService = paymentRecordService;
        this.alipayService = alipayService;
        this.messageService = messageService;
    }

    @Override
    @Transactional
    public PaymentRefund requestRefund(RentOrder order, PaymentRecord payment,
                                        String reason, String triggerType) {
        PaymentRefund existing = refundMapper.selectOne(
                Wrappers.<PaymentRefund>lambdaQuery()
                        .eq(PaymentRefund::getPaymentRecordId, payment.getId())
                        .last("LIMIT 1"));
        if (existing != null) return existing;

        LocalDateTime now = LocalDateTime.now();
        PaymentRefund refund = new PaymentRefund();
        refund.setId(UUID.randomUUID().toString());
        refund.setRefundNo("RF" + UUID.randomUUID().toString().replace("-", ""));
        refund.setPaymentRecordId(payment.getId());
        refund.setOrderId(order.getId());
        refund.setUserId(order.getUserId());
        refund.setRefundAmount(payment.getAmount());
        refund.setRefundReason(reason);
        refund.setTriggerType(triggerType);
        refund.setStatus("pending");
        refund.setAttemptCount(0);
        refund.setNextRetryAt(now);
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        refundMapper.insert(refund);

        payment.setStatus("refundPending");
        payment.setRemark("退款原因：" + reason);
        payment.setUpdatedAt(now);
        paymentRecordService.updateById(payment);

        order.setStatus("refundPending");
        order.setPaymentDeadlineAt(null);
        order.setLandlordSignDeadlineAt(null);
        order.setCancelReason(reason);
        order.setCancelledAt(now);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
        return refund;
    }

    @Override
    @Transactional
    public void processRefund(String refundId) {
        PaymentRefund refund = refundMapper.selectByIdForUpdate(refundId);
        if (refund == null || "success".equals(refund.getStatus())) return;

        PaymentRecord payment = paymentRecordService.getById(refund.getPaymentRecordId());
        if (payment == null) {
            markFailure(refund, "原支付记录不存在");
            return;
        }

        int attempts = refund.getAttemptCount() == null ? 0 : refund.getAttemptCount();
        try {
            boolean refunded;
            String tradeNo = payment.getChannelTradeNo();
            if ("mock".equals(payment.getPaymentChannel())) {
                refunded = true;
            } else if ("alipay".equals(payment.getPaymentChannel())) {
                AlipayService.AlipayRefundResult result = attempts > 0
                        ? alipayService.queryRefund(payment.getPaymentNo(), refund.getRefundNo())
                        : null;
                refunded = result != null && "REFUND_SUCCESS".equals(result.refundStatus());
                if (!refunded) {
                    result = alipayService.refund(payment.getPaymentNo(),
                            toAlipayAmount(refund.getRefundAmount()), refund.getRefundNo());
                    refunded = result != null && "Y".equals(result.fundChange());
                }
                if (result != null && result.tradeNo() != null) tradeNo = result.tradeNo();
            } else {
                markFailure(refund, "暂不支持支付渠道退款: " + payment.getPaymentChannel());
                return;
            }

            if (!refunded) {
                markFailure(refund, "退款结果未确认");
                return;
            }
            markSuccess(refund, payment, tradeNo);
        } catch (Exception ex) {
            log.error("处理支付退款失败 refundId={} refundNo={}", refundId, refund.getRefundNo(), ex);
            markFailure(refund, "退款调用异常");
        }
    }

    protected void markSuccess(PaymentRefund refund, PaymentRecord payment, String tradeNo) {
        LocalDateTime now = LocalDateTime.now();
        refund.setStatus("success");
        refund.setChannelTradeNo(tradeNo);
        refund.setAttemptCount((refund.getAttemptCount() == null ? 0 : refund.getAttemptCount()) + 1);
        refund.setLastError(null);
        refund.setNextRetryAt(null);
        refund.setRefundedAt(now);
        refund.setUpdatedAt(now);
        refundMapper.updateById(refund);

        payment.setStatus("refunded");
        payment.setUpdatedAt(now);
        paymentRecordService.updateById(payment);

        RentOrder order = orderMapper.selectByIdForUpdate(refund.getOrderId());
        if (order != null && !"completed".equals(order.getStatus())) {
            order.setStatus("refunded");
            order.setUpdatedAt(now);
            orderMapper.updateById(order);
        }
        messageService.sendMessage(
                refund.getUserId(),
                "bill",
                "退款成功",
                "租房订单退款已原路退回，退款金额为" + formatAmount(refund.getRefundAmount()) + "元。",
                "none",
                refund.getOrderId()
        );
        log.info("支付原路退款成功 orderId={} refundNo={} amount={}",
                refund.getOrderId(), refund.getRefundNo(), refund.getRefundAmount());
    }

    private void markFailure(PaymentRefund refund, String error) {
        int attempts = (refund.getAttemptCount() == null ? 0 : refund.getAttemptCount()) + 1;
        LocalDateTime now = LocalDateTime.now();
        refund.setAttemptCount(attempts);
        refund.setLastError(error);
        refund.setStatus(attempts >= MAX_ATTEMPTS ? "failed" : "processing");
        refund.setNextRetryAt(attempts >= MAX_ATTEMPTS ? null : now.plusSeconds(30));
        refund.setUpdatedAt(now);
        refundMapper.updateById(refund);
        if (attempts >= MAX_ATTEMPTS) {
            RentOrder order = orderMapper.selectById(refund.getOrderId());
            if (order != null && "refundPending".equals(order.getStatus())) {
                order.setStatus("refundFailed");
                order.setUpdatedAt(now);
                orderMapper.updateById(order);
            }
        }
    }

    static String toAlipayAmount(int amountInCents) {
        return BigDecimal.valueOf(amountInCents).movePointLeft(2)
                .setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String formatAmount(int amountInCents) {
        return BigDecimal.valueOf(amountInCents).movePointLeft(2)
                .setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
