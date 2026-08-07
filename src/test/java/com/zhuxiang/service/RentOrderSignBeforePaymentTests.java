package com.zhuxiang.service;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.mapper.UserRealNameAuthMapper;
import com.zhuxiang.service.service.*;
import com.zhuxiang.service.service.impl.RentOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RentOrderSignBeforePaymentTests {

    private final RentOrderMapper orderMapper = mock(RentOrderMapper.class);
    private final RentContractMapper contractMapper = mock(RentContractMapper.class);
    private final HouseService houseService = mock(HouseService.class);
    private final LeaseService leaseService = mock(LeaseService.class);
    private final PaymentRecordService paymentRecordService = mock(PaymentRecordService.class);
    private final RentBillService rentBillService = mock(RentBillService.class);
    private final EsignV3Client esignClient = mock(EsignV3Client.class);
    private final EsignV3Properties esignProperties = mock(EsignV3Properties.class);
    private RentOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RentOrderServiceImpl(
                houseService, contractMapper, leaseService, mock(UserService.class),
                mock(ApplicationEventPublisher.class), mock(FileRecordService.class),
                paymentRecordService, rentBillService, mock(AlipayService.class),
                mock(DepositService.class), new ObjectMapper(), mock(RealNameAuthService.class),
                mock(UserRealNameAuthMapper.class), mock(IdCardCryptoService.class), esignClient,
                esignProperties, mock(InspectionService.class), mock(CommunityService.class));
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);
        @SuppressWarnings("unchecked")
        LambdaUpdateChainWrapper<House> update = mock(LambdaUpdateChainWrapper.class,
                invocation -> "update".equals(invocation.getMethod().getName())
                        ? true : invocation.getMock());
        when(houseService.lambdaUpdate()).thenReturn(update);
    }

    @Test
    void confirmContractKeepsDraftUnlockedUntilUserClicksSign() {
        RentOrder order = order("pendingContract");
        RentContract contract = contract("generated");
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(contractMapper.selectOne(any(), eq(false))).thenReturn(contract);

        service.confirmContract("tenant-1", "order-1");

        assertThat(order.getStatus()).isEqualTo("pendingContract");
        assertThat(order.getContractConfirmedAt()).isNull();
        assertThat(contract.getStatus()).isEqualTo("generated");
    }

    @Test
    void tenantSignClickAcquiresReservationAndStartsFiveMinuteWindow() {
        RentOrder order = order("pendingContract");
        order.setPrePaymentDeadlineAt(LocalDateTime.now().plusMinutes(5));
        House house = new House();
        house.setId("house-1");
        house.setStatus("available");
        when(houseService.getById("house-1")).thenReturn(house);

        Boolean acquired = ReflectionTestUtils.invokeMethod(
                service, "ensureTenantSigningReservation", order);

        assertThat(acquired).isTrue();
        assertThat(order.getPrePaymentDeadlineAt())
                .isAfter(LocalDateTime.now().plusMinutes(4));
        verify(houseService.lambdaUpdate()).update();
        verify(orderMapper).updateById(order);
    }

    @Test
    void successfulPaymentOpensLandlordSigningWithoutCreatingLease() {
        RentOrder order = order("pendingPayment");
        order.setPaymentDeadlineAt(LocalDateTime.now().plusMinutes(14));
        PaymentRecord record = paymentRecord();
        RentContract contract = contract("signing");
        contract.setTenantSigned(1);

        when(paymentRecordService.getById("payment-1")).thenReturn(record);
        when(paymentRecordService.getOne(any(), eq(false))).thenReturn(record);
        when(orderMapper.selectByIdForUpdate("order-1")).thenReturn(order);
        when(contractMapper.selectByOrderIdForUpdate("order-1")).thenReturn(contract);
        House house = new House();
        house.setId("house-1");
        house.setStatus("reserved");
        when(houseService.getById("house-1")).thenReturn(house);

        service.confirmPayment("payment-1", "trade-1");

        assertThat(record.getStatus()).isEqualTo("success");
        assertThat(order.getStatus()).isEqualTo("pendingLandlordSign");
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(house.getStatus()).isEqualTo("reserved");
        verify(orderMapper, atLeastOnce()).selectByIdForUpdate("order-1");
        verify(leaseService, never()).save(any(Lease.class));
        verify(rentBillService, never()).save(any());
    }

    @Test
    void latePaymentCallbackGoesToRefundHandlingWithoutLease() {
        RentOrder order = order("paymentExpired");
        PaymentRecord record = paymentRecord();
        when(paymentRecordService.getById("payment-1")).thenReturn(record);
        when(paymentRecordService.getOne(any(), eq(false))).thenReturn(record);
        when(orderMapper.selectByIdForUpdate("order-1")).thenReturn(order);

        service.confirmPayment("payment-1", "late-trade");

        assertThat(record.getStatus()).isEqualTo("refundPending");
        assertThat(record.getChannelTradeNo()).isEqualTo("late-trade");
        verify(leaseService, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void timeoutExpiresOrderWithoutCallingEsignAndReleasesReservedHouse() {
        RentOrder order = order("pendingPayment");
        order.setPaymentDeadlineAt(LocalDateTime.now().minusMinutes(1));
        when(orderMapper.selectByIdForUpdate("order-1")).thenReturn(order);
        LambdaUpdateChainWrapper<House> update = houseService.lambdaUpdate();

        service.processPaymentTimeout("order-1");
        service.processPaymentTimeout("order-1");

        assertThat(order.getStatus()).isEqualTo("paymentExpired");
        assertThat(order.getCancelReason()).isEqualTo("PAYMENT_TIMEOUT");
        verifyNoInteractions(esignClient);
        verify(update).update();
    }

    @Test
    void tenantSignatureOpensFifteenMinutePaymentWindow() {
        RentOrder order = order("pendingTenantSign");
        RentContract contract = contract("signing");
        contract.setSignFlowId("flow-1");
        contract.setTenantPhone("13800138000");
        contract.setLandlordPhone("13900139000");
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(orderMapper.selectByIdForUpdate("order-1")).thenReturn(order);
        when(contractMapper.selectByOrderIdForUpdate("order-1")).thenReturn(contract);

        EsignV3Client.SignFlowDetailResponse detail = new EsignV3Client.SignFlowDetailResponse();
        EsignV3Client.SignFlowDetailResponse.SignFlowDetailData data =
                new EsignV3Client.SignFlowDetailResponse.SignFlowDetailData();
        data.setSignFlowStatus(1);
        EsignV3Client.SignFlowDetailResponse.SignerDetail tenant =
                new EsignV3Client.SignFlowDetailResponse.SignerDetail();
        tenant.setSignOrder(1);
        tenant.setSignStatus(2);
        EsignV3Client.SignFlowDetailResponse.PsnAccount tenantAccount =
                new EsignV3Client.SignFlowDetailResponse.PsnAccount();
        tenantAccount.setAccountMobile("13800138000");
        EsignV3Client.SignFlowDetailResponse.PsnSigner tenantSigner =
                new EsignV3Client.SignFlowDetailResponse.PsnSigner();
        tenantSigner.setPsnAccount(tenantAccount);
        tenant.setPsnSigner(tenantSigner);
        data.setSigners(java.util.List.of(tenant));
        detail.setData(data);
        when(esignClient.getSignFlowDetail("flow-1")).thenReturn(detail);

        service.contractRefresh("tenant-1", "order-1");

        assertThat(contract.getTenantSigned()).isEqualTo(1);
        assertThat(contract.getLessorSigned()).isEqualTo(0);
        assertThat(order.getStatus()).isEqualTo("pendingPayment");
        assertThat(order.getPaymentDeadlineAt()).isAfter(LocalDateTime.now().plusMinutes(14));
        assertThat(order.getSignedAt()).isNull();
    }

    @Test
    void contractRefreshBeforeSignFlowReturnsNotStarted() {
        RentOrder order = order("pendingTenantSign");
        RentContract contract = contract("confirmed");
        when(orderMapper.selectByIdForUpdate("order-1")).thenReturn(order);
        when(contractMapper.selectByOrderIdForUpdate("order-1")).thenReturn(contract);

        var response = service.contractRefresh("tenant-1", "order-1");

        assertThat(response.contractStatus()).isEqualTo("NOT_STARTED");
        assertThat(response.tenantSigned()).isFalse();
        assertThat(response.lessorSigned()).isFalse();
        verifyNoInteractions(esignClient);
    }

    @Test
    void landlordCannotGetSigningLinkBeforePayment() {
        RentOrder order = order("pendingTenantSign");
        when(orderMapper.selectById("order-1")).thenReturn(order);

        assertThatThrownBy(() -> service.sign("landlord-1", "order-1"))
                .hasMessageContaining("支付完成后才允许房东签约");
        verifyNoInteractions(esignClient);
    }

    @Test
    void tenantCannotSignAgainAfterEnteringPaymentStage() {
        RentOrder order = order("pendingPayment");
        when(orderMapper.selectById("order-1")).thenReturn(order);

        assertThatThrownBy(() -> service.sign("tenant-1", "order-1"))
                .hasMessageContaining("不允许租客签约");
    }

    private RentOrder order(String status) {
        RentOrder order = new RentOrder();
        order.setId("order-1");
        order.setUserId("tenant-1");
        order.setLessorUserId("landlord-1");
        order.setHouseId("house-1");
        order.setStatus(status);
        order.setStartDate(LocalDate.now().plusDays(10));
        order.setEndDate(LocalDate.now().plusMonths(2));
        order.setLeaseMonths(2);
        order.setPaymentMethod("monthly");
        order.setPaymentMonths(1);
        order.setMonthlyRent(300000);
        order.setDeposit(300000);
        order.setServiceFee(20000);
        order.setFirstPaymentAmount(620000);
        return order;
    }

    private RentContract contract(String status) {
        RentContract contract = new RentContract();
        contract.setId("contract-1");
        contract.setOrderId("order-1");
        contract.setStatus(status);
        return contract;
    }

    private PaymentRecord paymentRecord() {
        PaymentRecord record = new PaymentRecord();
        record.setId("payment-1");
        record.setOrderId("order-1");
        record.setStatus("pending");
        return record;
    }
}
