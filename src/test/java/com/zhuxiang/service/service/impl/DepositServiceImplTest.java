package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.entity.DepositRecord;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.mapper.DepositDeductionMapper;
import com.zhuxiang.service.service.AlipayService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepositServiceImplTest {

    @Test
    void convertsCentsToAlipayYuanAmount() {
        assertEquals("2800.00", DepositServiceImpl.toAlipayAmount(280000));
        assertEquals("0.01", DepositServiceImpl.toAlipayAmount(1));
    }

    @Test
    void doesNotRecordSuccessWhenAlipayRefundResultIsUnknown() {
        PaymentRecordService paymentRecordService = mock(PaymentRecordService.class);
        AlipayService alipayService = mock(AlipayService.class);
        DepositServiceImpl service = spy(new DepositServiceImpl(
                mock(DepositDeductionMapper.class),
                paymentRecordService,
                alipayService,
                mock(UserService.class),
                mock(HouseService.class)
        ));

        DepositRecord deposit = new DepositRecord();
        deposit.setId("12345678-1234-1234-1234-123456789012");
        deposit.setPaymentRecordId("payment-1");
        deposit.setUserId("user-1");
        deposit.setAmount(280000);
        deposit.setWithheldAmount(0);
        deposit.setStatus("deducted");
        doReturn(deposit).when(service).getById(deposit.getId());
        doReturn(true).when(service).updateById(any(DepositRecord.class));

        PaymentRecord payment = new PaymentRecord();
        payment.setId("payment-1");
        payment.setPaymentNo("ZF001");
        payment.setChannelTradeNo("ALI001");
        payment.setPaymentChannel("alipay");
        payment.setStatus("success");
        when(paymentRecordService.getById("payment-1")).thenReturn(payment);
        when(alipayService.refund(eq("ZF001"), eq("2800.00"), any())).thenReturn(null);

        service.refund(deposit.getId());

        assertEquals("refunding", deposit.getStatus());
        verify(paymentRecordService, never()).save(any(PaymentRecord.class));
    }

    @Test
    void refundRecordInheritsRequiredOrderIdFromOriginalPayment() {
        PaymentRecordService paymentRecordService = mock(PaymentRecordService.class);
        AlipayService alipayService = mock(AlipayService.class);
        DepositServiceImpl service = spy(new DepositServiceImpl(
                mock(DepositDeductionMapper.class),
                paymentRecordService,
                alipayService,
                mock(UserService.class),
                mock(HouseService.class)
        ));

        DepositRecord deposit = new DepositRecord();
        deposit.setId("12345678-1234-1234-1234-123456789012");
        deposit.setPaymentRecordId("payment-1");
        deposit.setUserId("user-1");
        deposit.setLeaseId("lease-1");
        deposit.setHouseId("house-1");
        deposit.setAmount(280000);
        deposit.setWithheldAmount(30000);
        deposit.setStatus("deducted");
        doReturn(deposit).when(service).getById(deposit.getId());
        doReturn(true).when(service).updateById(any(DepositRecord.class));

        PaymentRecord payment = new PaymentRecord();
        payment.setId("payment-1");
        payment.setOrderId("order-1");
        payment.setPaymentNo("ZF001");
        payment.setChannelTradeNo("ALI001");
        payment.setPaymentChannel("alipay");
        payment.setStatus("success");
        when(paymentRecordService.getById("payment-1")).thenReturn(payment);
        when(paymentRecordService.generatePaymentNo()).thenReturn("TK001");
        when(alipayService.refund(eq("ZF001"), eq("2500.00"), any()))
                .thenReturn(new AlipayService.AlipayRefundResult(
                        "ALI001", "ZF001", "2500.00", "RFD001", "Y", null));

        service.refund(deposit.getId());

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordService).save(captor.capture());
        assertEquals("order-1", captor.getValue().getOrderId());
        assertEquals("refund", captor.getValue().getType());
        assertEquals("refunded", deposit.getStatus());
    }
}
