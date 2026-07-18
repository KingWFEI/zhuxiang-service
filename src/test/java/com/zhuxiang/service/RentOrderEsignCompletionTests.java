package com.zhuxiang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.dto.EsignSignStatusResponse;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.mapper.UserRealNameAuthMapper;
import com.zhuxiang.service.service.AlipayService;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.DepositService;
import com.zhuxiang.service.service.EsignCallbackData;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.IdCardCryptoService;
import com.zhuxiang.service.service.InspectionService;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.RealNameAuthService;
import com.zhuxiang.service.service.RentBillService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.RentOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentOrderEsignCompletionTests {

    @Test
    void repeatedCompletedCallbackRepairsSignedPartyFlags() {
        RentOrderMapper orderMapper = mock(RentOrderMapper.class);
        RentContractMapper contractMapper = mock(RentContractMapper.class);
        RentOrderServiceImpl service = new RentOrderServiceImpl(
                mock(HouseService.class), contractMapper, mock(LeaseService.class), mock(UserService.class),
                mock(ApplicationEventPublisher.class), mock(FileRecordService.class),
                mock(PaymentRecordService.class), mock(RentBillService.class), mock(AlipayService.class),
                mock(DepositService.class), new ObjectMapper(), mock(RealNameAuthService.class),
                mock(UserRealNameAuthMapper.class), mock(IdCardCryptoService.class), mock(EsignV3Client.class),
                mock(EsignV3Properties.class), mock(InspectionService.class), mock(CommunityService.class));
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);

        RentContract contract = new RentContract();
        contract.setId("contract-1");
        contract.setOrderId("order-1");
        contract.setSignFlowId("flow-1");
        contract.setStatus("signed");
        contract.setLessorSigned(0);
        contract.setTenantSigned(0);
        when(contractMapper.selectOne(any(), eq(false))).thenReturn(contract);

        EsignCallbackData callback = new EsignCallbackData();
        callback.setSignFlowId("flow-1");
        callback.setSignFlowStatus(2);

        service.processEsignCallback(callback);

        assertThat(contract.getLessorSigned()).isEqualTo(1);
        assertThat(contract.getTenantSigned()).isEqualTo(1);
        verify(contractMapper).updateById(contract);
    }

    @Test
    void completedRefreshMarksBothPartiesSignedWithoutSignerRoles() {
        RentOrderMapper orderMapper = mock(RentOrderMapper.class);
        RentContractMapper contractMapper = mock(RentContractMapper.class);
        HouseService houseService = mock(HouseService.class);
        LeaseService leaseService = mock(LeaseService.class);
        RentBillService rentBillService = mock(RentBillService.class);
        EsignV3Client esignClient = mock(EsignV3Client.class);
        RentOrderServiceImpl service = new RentOrderServiceImpl(
                houseService, contractMapper, leaseService, mock(UserService.class),
                mock(ApplicationEventPublisher.class), mock(FileRecordService.class),
                mock(PaymentRecordService.class), rentBillService, mock(AlipayService.class),
                mock(DepositService.class), new ObjectMapper(), mock(RealNameAuthService.class),
                mock(UserRealNameAuthMapper.class), mock(IdCardCryptoService.class), esignClient,
                mock(EsignV3Properties.class), mock(InspectionService.class), mock(CommunityService.class));
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);

        RentOrder order = new RentOrder();
        order.setId("order-1");
        order.setUserId("tenant-1");
        order.setHouseId("house-1");
        order.setStatus("pendingEsign");
        order.setStartDate(LocalDate.now().plusDays(10));
        order.setEndDate(LocalDate.now().plusMonths(2));
        order.setLeaseMonths(2);
        order.setPaymentMethod("monthly");
        order.setPaymentMonths(1);
        order.setMonthlyRent(300000);
        order.setDeposit(300000);
        order.setServiceFee(20000);
        order.setFirstPaymentAmount(620000);
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(orderMapper.selectOne(any())).thenReturn(order);

        RentContract contract = new RentContract();
        contract.setId("contract-1");
        contract.setOrderId("order-1");
        contract.setSignFlowId("flow-1");
        contract.setStatus("signing");
        when(contractMapper.selectOne(any(), eq(false))).thenReturn(contract);

        EsignV3Client.SignFlowDetailResponse detail = new EsignV3Client.SignFlowDetailResponse();
        EsignV3Client.SignFlowDetailResponse.SignFlowDetailData data =
                new EsignV3Client.SignFlowDetailResponse.SignFlowDetailData();
        data.setSignFlowStatus(2);
        detail.setData(data);
        when(esignClient.getSignFlowDetail("flow-1")).thenReturn(detail);

        House house = new House();
        house.setId("house-1");
        when(houseService.getById("house-1")).thenReturn(house);
        when(leaseService.save(any(Lease.class))).thenReturn(true);
        when(rentBillService.save(any())).thenReturn(true);

        EsignSignStatusResponse response = service.contractRefresh("tenant-1", "order-1");

        assertThat(response.contractStatus()).isEqualTo("COMPLETED");
        assertThat(response.lessorSigned()).isTrue();
        assertThat(response.tenantSigned()).isTrue();
        assertThat(contract.getLessorSigned()).isEqualTo(1);
        assertThat(contract.getTenantSigned()).isEqualTo(1);
    }

    @Test
    void completedSignFlowFinishesOrderAndCreatesPendingLease() {
        RentOrderMapper orderMapper = mock(RentOrderMapper.class);
        RentContractMapper contractMapper = mock(RentContractMapper.class);
        HouseService houseService = mock(HouseService.class);
        LeaseService leaseService = mock(LeaseService.class);
        RentBillService rentBillService = mock(RentBillService.class);
        DepositService depositService = mock(DepositService.class);
        InspectionService inspectionService = mock(InspectionService.class);

        RentOrderServiceImpl service = new RentOrderServiceImpl(
                houseService,
                contractMapper,
                leaseService,
                mock(UserService.class),
                mock(ApplicationEventPublisher.class),
                mock(FileRecordService.class),
                mock(PaymentRecordService.class),
                rentBillService,
                mock(AlipayService.class),
                depositService,
                new ObjectMapper(),
                mock(RealNameAuthService.class),
                mock(UserRealNameAuthMapper.class),
                mock(IdCardCryptoService.class),
                mock(EsignV3Client.class),
                mock(EsignV3Properties.class),
                inspectionService,
                mock(CommunityService.class));
        ReflectionTestUtils.setField(service, "baseMapper", orderMapper);

        RentContract contract = new RentContract();
        contract.setId("contract-1");
        contract.setOrderId("order-1");
        contract.setSignFlowId("flow-1");
        contract.setStatus("signing");
        when(contractMapper.selectOne(any(), eq(false))).thenReturn(contract);

        RentOrder order = new RentOrder();
        order.setId("order-1");
        order.setUserId("tenant-1");
        order.setHouseId("house-1");
        order.setStatus("pendingEsign");
        order.setStartDate(LocalDate.now().plusDays(10));
        order.setEndDate(LocalDate.now().plusMonths(2));
        order.setLeaseMonths(2);
        order.setPaymentMethod("monthly");
        order.setPaymentMonths(1);
        order.setMonthlyRent(300000);
        order.setDeposit(300000);
        order.setServiceFee(20000);
        order.setFirstPaymentAmount(620000);
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(orderMapper.selectOne(any())).thenReturn(order);

        House house = new House();
        house.setId("house-1");
        house.setStatus("reserved");
        when(houseService.getById("house-1")).thenReturn(house);
        when(leaseService.save(any(Lease.class))).thenReturn(true);
        when(rentBillService.save(any())).thenReturn(true);

        EsignCallbackData callback = new EsignCallbackData();
        callback.setSignFlowId("flow-1");
        callback.setSignFlowStatus(2);
        callback.setContractNum("CN-1");

        service.processEsignCallback(callback);

        assertThat(contract.getStatus()).isEqualTo("signed");
        assertThat(contract.getTenantSigned()).isEqualTo(1);
        assertThat(contract.getLessorSigned()).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo("completed");
        assertThat(house.getStatus()).isEqualTo("rented");
        verify(leaseService).save(any(Lease.class));
        verify(rentBillService, times(2)).save(any());
        verify(inspectionService).createSnapshotFromTemplate(
                eq("contract-1"), any(String.class), eq("house-1"));
    }
}
