package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.entity.PaymentRefund;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.mapper.PaymentRefundMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.AlipayService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.MessageService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRefundServiceImplTest {

    @Test
    void convertsCentsToAlipayYuanAmount() {
        assertEquals("2800.00", PaymentRefundServiceImpl.toAlipayAmount(280000));
        assertEquals("0.01", PaymentRefundServiceImpl.toAlipayAmount(1));
    }

    @Test
    void marksPaymentAndOrderRefundedOnlyAfterAlipayConfirmsFundChange() {
        PaymentRefundMapper refundMapper = mock(PaymentRefundMapper.class);
        RentOrderMapper orderMapper = mock(RentOrderMapper.class);
        PaymentRecordService paymentService = mock(PaymentRecordService.class);
        AlipayService alipayService = mock(AlipayService.class);
        MessageService messageService = mock(MessageService.class);
        PaymentRefundServiceImpl service = new PaymentRefundServiceImpl(
                refundMapper, orderMapper, paymentService, alipayService, messageService);

        PaymentRefund refund = refund("refund-1");
        PaymentRecord payment = payment();
        RentOrder order = new RentOrder();
        order.setId("order-1");
        order.setStatus("refundPending");
        when(refundMapper.selectByIdForUpdate("refund-1")).thenReturn(refund);
        when(paymentService.getById("payment-1")).thenReturn(payment);
        when(orderMapper.selectByIdForUpdate("order-1")).thenReturn(order);
        when(alipayService.refund("ZF001", "2800.00", "RF001"))
                .thenReturn(new AlipayService.AlipayRefundResult(
                        "ALI001", "ZF001", "2800.00", "RF001", "Y", null));

        service.processRefund("refund-1");

        assertEquals("success", refund.getStatus());
        assertEquals("refunded", payment.getStatus());
        assertEquals("refunded", order.getStatus());
        verify(refundMapper).updateById(refund);
        verify(paymentService).updateById(payment);
        verify(orderMapper).updateById(order);
        verify(messageService).sendMessage(
                "user-1", "bill", "退款成功",
                "租房订单退款已原路退回，退款金额为2800.00元。",
                "none", "order-1");
    }

    @Test
    void keepsOrderPendingWhenAlipayDoesNotConfirmRefund() {
        PaymentRefundMapper refundMapper = mock(PaymentRefundMapper.class);
        RentOrderMapper orderMapper = mock(RentOrderMapper.class);
        PaymentRecordService paymentService = mock(PaymentRecordService.class);
        AlipayService alipayService = mock(AlipayService.class);
        MessageService messageService = mock(MessageService.class);
        PaymentRefundServiceImpl service = new PaymentRefundServiceImpl(
                refundMapper, orderMapper, paymentService, alipayService, messageService);

        PaymentRefund refund = refund("refund-2");
        PaymentRecord payment = payment();
        when(refundMapper.selectByIdForUpdate("refund-2")).thenReturn(refund);
        when(paymentService.getById("payment-1")).thenReturn(payment);
        when(alipayService.refund("ZF001", "2800.00", "RF001"))
                .thenReturn(new AlipayService.AlipayRefundResult(
                        "ALI001", "ZF001", "2800.00", "RF001", "N", null));

        service.processRefund("refund-2");

        assertEquals("processing", refund.getStatus());
        assertEquals(1, refund.getAttemptCount());
        assertEquals("success", payment.getStatus());
        assertNull(refund.getRefundedAt());
        verify(paymentService, never()).updateById(payment);
        verify(orderMapper, never()).updateById(org.mockito.ArgumentMatchers.any(RentOrder.class));
        verify(messageService, never()).sendMessage(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static PaymentRefund refund(String id) {
        PaymentRefund refund = new PaymentRefund();
        refund.setId(id);
        refund.setRefundNo("RF001");
        refund.setPaymentRecordId("payment-1");
        refund.setOrderId("order-1");
        refund.setUserId("user-1");
        refund.setRefundAmount(280000);
        refund.setStatus("pending");
        refund.setAttemptCount(0);
        return refund;
    }

    private static PaymentRecord payment() {
        PaymentRecord payment = new PaymentRecord();
        payment.setId("payment-1");
        payment.setPaymentNo("ZF001");
        payment.setPaymentChannel("alipay");
        payment.setStatus("success");
        return payment;
    }
}
