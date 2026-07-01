package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.dto.LeaseTerminationDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.LeaseTerminationApplication;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.mapper.LeaseTerminationApplicationMapper;
import com.zhuxiang.service.mapper.LeaseTerminationLogMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.service.LockPasscodePermissionService;
import com.zhuxiang.service.service.LockPermissionService;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.RentBillService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.service.impl.LeaseTerminationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaseTerminationServiceTests {

    private final RentContractMapper contractMapper = mock(RentContractMapper.class);
    private final LeaseService leaseService = mock(LeaseService.class);
    private final RentBillService billService = mock(RentBillService.class);
    private final HouseService houseService = mock(HouseService.class);
    private final MessageService messageService = mock(MessageService.class);
    private final UserService userService = mock(UserService.class);
    private final LockPermissionService lockPermissionService = mock(LockPermissionService.class);
    private final LockPasscodePermissionService passcodePermissionService = mock(LockPasscodePermissionService.class);
    private final LeaseTerminationLogMapper logMapper = mock(LeaseTerminationLogMapper.class);
    private final LeaseTerminationApplicationMapper applicationMapper = mock(LeaseTerminationApplicationMapper.class);
    private LeaseTerminationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LeaseTerminationServiceImpl(
                contractMapper, leaseService, billService, houseService,
                messageService, userService, lockPermissionService,
                passcodePermissionService, logMapper, new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "baseMapper", applicationMapper);
        when(applicationMapper.insert(any(LeaseTerminationApplication.class))).thenReturn(1);
        when(applicationMapper.updateById(any(LeaseTerminationApplication.class))).thenReturn(1);
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
    }

    @Test
    void createsTerminationApplicationFromLeaseId() {
        Lease lease = activeLease();
        RentContract contract = signedContract();
        when(leaseService.getById("lease-1")).thenReturn(lease);
        when(contractMapper.selectById("contract-1")).thenReturn(contract);

        LeaseTerminationDtos.ApplyResponse result = service.apply(
                "tenant-1", "lease-1", applyRequest()
        );

        ArgumentCaptor<LeaseTerminationApplication> captor =
                ArgumentCaptor.forClass(LeaseTerminationApplication.class);
        verify(applicationMapper).insert(captor.capture());
        LeaseTerminationApplication saved = captor.getValue();
        assertThat(saved.getLeaseId()).isEqualTo("lease-1");
        assertThat(saved.getContractId()).isEqualTo("contract-1");
        assertThat(saved.getHouseId()).isEqualTo("house-1");
        assertThat(saved.getStatus()).isEqualTo("pending_review");
        assertThat(result.leaseId()).isEqualTo("lease-1");
        verify(messageService).sendMessage(
                eq("tenant-1"), eq("lease"), eq("退租申请已提交"),
                any(String.class), eq("lease"), eq(saved.getId())
        );
    }

    @Test
    void currentApplicationFallsBackToLegacyContractLinkedRecord() {
        Lease lease = activeLease();
        LeaseTerminationApplication legacy = application("pending_review");
        legacy.setLeaseId(null);
        when(leaseService.getById("lease-1")).thenReturn(lease);
        when(applicationMapper.selectOne(any(Wrapper.class), eq(false)))
                .thenReturn(null, legacy);

        LeaseTerminationDtos.TerminationDetailResponse result =
                service.getCurrent("tenant-1", "lease-1");

        assertThat(result.id()).isEqualTo(legacy.getId());
        assertThat(result.contractId()).isEqualTo("contract-1");
    }

    @Test
    void approvalOnlyStartsInspectionAndDoesNotTerminateLease() {
        LeaseTerminationApplication application = application("pending_review");
        when(applicationMapper.selectById(application.getId())).thenReturn(application);

        service.approve("admin-1", application.getId());

        assertThat(application.getStatus()).isEqualTo("inspection_pending");
        verify(lockPermissionService, never()).revokeTenantEKeyForLease(any(String.class));
        verify(passcodePermissionService, never()).revokePasscodesForLease(any(String.class));
        verify(contractMapper, never()).updateById(any(RentContract.class));
    }

    @Test
    void completionTerminatesLeaseContractReleasesHouseAndRevokesLocks() {
        LeaseTerminationApplication application = application("settlement_pending");
        application.setRefundAmount(0);
        Lease lease = activeLease();
        RentContract contract = signedContract();
        House house = new House();
        house.setId("house-1");
        house.setStatus("rented");
        when(applicationMapper.selectById(application.getId())).thenReturn(application);
        when(leaseService.getById("lease-1")).thenReturn(lease);
        when(contractMapper.selectById("contract-1")).thenReturn(contract);
        when(houseService.getById("house-1")).thenReturn(house);

        service.confirmSettlement(
                "admin-1",
                application.getId(),
                new LeaseTerminationDtos.SettlementConfirmRequest(10000, 0, "扣除清洁费用")
        );

        assertThat(application.getStatus()).isEqualTo("completed");
        assertThat(application.getSettlementDetail()).isEqualTo(
                "{\"settlementAmount\":10000,\"refundAmount\":0,\"remark\":\"扣除清洁费用\"}"
        );
        assertThat(lease.getStatus()).isEqualTo("terminated");
        assertThat(contract.getStatus()).isEqualTo("terminated");
        assertThat(house.getStatus()).isEqualTo("available");
        verify(leaseService).updateById(lease);
        verify(contractMapper).updateById(contract);
        verify(houseService).updateById(house);
        verify(lockPermissionService).revokeTenantEKeyForLease("lease-1");
        verify(passcodePermissionService).revokePasscodesForLease("lease-1");
    }

    private Lease activeLease() {
        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setUserId("tenant-1");
        lease.setHouseId("house-1");
        lease.setContractId("contract-1");
        lease.setStatus("active");
        lease.setStartDate(LocalDate.of(2026, 7, 1));
        lease.setEndDate(LocalDate.of(2027, 6, 30));
        return lease;
    }

    private RentContract signedContract() {
        RentContract contract = new RentContract();
        contract.setId("contract-1");
        contract.setUserId("tenant-1");
        contract.setHouseId("house-1");
        contract.setStatus("signed");
        contract.setDeposit(280000);
        contract.setEndDate(LocalDate.of(2027, 6, 30));
        return contract;
    }

    private LeaseTerminationApplication application(String status) {
        LeaseTerminationApplication application = new LeaseTerminationApplication();
        application.setId("application-1");
        application.setApplicationNo("TZ202606300001");
        application.setTenantId("tenant-1");
        application.setLeaseId("lease-1");
        application.setContractId("contract-1");
        application.setHouseId("house-1");
        application.setStatus(status);
        application.setReason("工作变动");
        application.setAttachments(null);
        return application;
    }

    private LeaseTerminationDtos.ApplyRequest applyRequest() {
        return new LeaseTerminationDtos.ApplyRequest(
                "工作变动",
                LocalDate.of(2026, 8, 1),
                false,
                "张三",
                "13800138000",
                "请联系我确认",
                List.of(new LeaseTerminationDtos.AttachmentItem(
                        "/api/uploads/termination.jpg", "image", "材料照片"
                ))
        );
    }
}
