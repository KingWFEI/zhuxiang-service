package com.zhuxiang.service;

import com.zhuxiang.service.config.AlipayProperties;
import com.zhuxiang.service.dto.BillDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.entity.RentBill;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.impl.AlipayServiceImpl;
import com.zhuxiang.service.service.impl.BillServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillPaymentServiceTests {

    private final LeaseService leaseService = mock(LeaseService.class);
    private final HouseService houseService = mock(HouseService.class);
    private final PaymentRecordService paymentRecordService = mock(PaymentRecordService.class);
    private final com.zhuxiang.service.service.AlipayService alipayService =
            mock(com.zhuxiang.service.service.AlipayService.class);
    private BillServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new BillServiceImpl(
                leaseService, houseService, paymentRecordService, alipayService));
        RentBill bill = new RentBill();
        bill.setId("bill-1");
        bill.setLeaseId("lease-1");
        bill.setPeriodNo(1);
        bill.setAmountDue(100);
        bill.setAmountPaid(0);
        bill.setOverdueAmount(0);
        bill.setDueDate(LocalDate.now());
        bill.setStatus("pending");
        doReturn(bill).when(service).getById("bill-1");

        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setUserId("user-1");
        lease.setHouseId("house-1");
        when(leaseService.getById("lease-1")).thenReturn(lease);
        House house = new House();
        house.setTitle("测试房源");
        when(houseService.getById("house-1")).thenReturn(house);
        when(paymentRecordService.generatePaymentNo()).thenReturn("ZF001");
        when(paymentRecordService.save(any(PaymentRecord.class))).thenReturn(true);
    }

    @Test
    void appModeReturnsOrderStringInsteadOfH5Url() {
        when(alipayService.getPayType()).thenReturn("app");
        when(alipayService.buildPayPayload("ZF001", 100, "勿忧管家租房-第1期租金-测试房源"))
                .thenReturn("signed-app-order");

        BillDtos.BillPayResponse result = service.payBill(
                "user-1", "bill-1", new BillDtos.BillPayRequest(null));

        assertThat(result.payType()).isEqualTo("app");
        assertThat(result.paymentUrl()).isNull();
        assertThat(result.orderString()).isEqualTo("signed-app-order");
        verify(alipayService).validatePaymentChannel("alipay");
    }

    @Test
    void disabledMockChannelFailsBeforePaymentRecordIsCreated() {
        AlipayProperties properties = new AlipayProperties();
        AlipayServiceImpl realPolicy = new AlipayServiceImpl(properties);
        BillServiceImpl policyService = spy(new BillServiceImpl(
                leaseService, houseService, paymentRecordService, realPolicy));
        doReturn(service.getById("bill-1")).when(policyService).getById("bill-1");

        assertThatThrownBy(() -> policyService.payBill(
                "user-1", "bill-1", new BillDtos.BillPayRequest("mock")))
                .hasMessageContaining("未启用");
        verify(paymentRecordService, never()).save(any(PaymentRecord.class));
    }
}
