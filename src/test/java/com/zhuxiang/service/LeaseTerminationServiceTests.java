package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.dto.LeaseTerminationDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.LeaseTerminationApplication;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.entity.UserRealNameAuth;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.common.EsignException;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.mapper.LeaseTerminationApplicationMapper;
import com.zhuxiang.service.mapper.LeaseTerminationLogMapper;
import com.zhuxiang.service.mapper.UserRealNameAuthMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.service.LockPasscodePermissionService;
import com.zhuxiang.service.service.LockPermissionService;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.DepositService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private final DepositService depositService = mock(DepositService.class);
    private final EsignV3Client esignV3Client = mock(EsignV3Client.class);
    private final EsignV3Properties esignProperties = mock(EsignV3Properties.class);
    private final UserRealNameAuthMapper realNameAuthMapper = mock(UserRealNameAuthMapper.class);
    private LeaseTerminationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LeaseTerminationServiceImpl(
                contractMapper, leaseService, billService, houseService,
                messageService, userService, lockPermissionService,
                passcodePermissionService, logMapper, depositService, new ObjectMapper(),
                esignV3Client, esignProperties, realNameAuthMapper
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
        assertThat(saved.getStatus()).isEqualTo("pending_photos");
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
    void photoSubmissionAdvancesApplicationToOfflineInspection() {
        LeaseTerminationApplication application = application("pending_photos");
        when(applicationMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(application);

        service.markPhotosSubmitted("tenant-1", "contract-1");

        assertThat(application.getStatus()).isEqualTo("inspection_pending");
        verify(applicationMapper).updateById(application);
    }

    @Test
    void inspectionCompletionStoresValidJsonForLegacyApplication() throws Exception {
        LeaseTerminationApplication application = application("inspection_pending");
        when(applicationMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(application);

        service.completeInspectionByContract("admin-1", "contract-1", "线下验房完成");

        assertThat(application.getStatus()).isEqualTo("settlement_pending");
        var result = new ObjectMapper().readTree(application.getInspectionResult());
        assertThat(result.path("completedBy").asText()).isEqualTo("admin-1");
        assertThat(result.path("comment").asText()).isEqualTo("线下验房完成");
        assertThat(result.path("completedAt").asText()).isNotBlank();
    }

    @Test
    void adminCanCancelBeforeSettlementWithoutEndingLease() {
        LeaseTerminationApplication application = application("inspection_pending");
        when(applicationMapper.selectById(application.getId())).thenReturn(application);

        service.adminCancel(
                "admin-1",
                application.getId(),
                new LeaseTerminationDtos.CancelRequest("租客线下确认继续承租")
        );

        assertThat(application.getStatus()).isEqualTo("cancelled");
        assertThat(application.getCancelReason()).isEqualTo("租客线下确认继续承租");
        assertThat(application.getCancelTime()).isNotNull();
        verify(leaseService, never()).updateById(any(Lease.class));
        verify(lockPermissionService, never()).revokeTenantEKeyForLease(any(String.class));
    }

    @Test
    void adminCannotCancelAfterSettlementStarts() {
        LeaseTerminationApplication application = application("settlement_pending");
        when(applicationMapper.selectById(application.getId())).thenReturn(application);

        assertThatThrownBy(() -> service.adminCancel(
                "admin-1",
                application.getId(),
                new LeaseTerminationDtos.CancelRequest("尝试撤销")
        )).hasMessageContaining("不允许撤销");
    }

    @Test
    void adjustedRefundRequiresAuditReason() {
        LeaseTerminationApplication application = application("settlement_pending");
        com.zhuxiang.service.entity.DepositRecord deposit = new com.zhuxiang.service.entity.DepositRecord();
        deposit.setId("deposit-1");
        deposit.setAmount(280000);
        deposit.setStatus("held");
        when(applicationMapper.selectById(application.getId())).thenReturn(application);
        when(depositService.getByLeaseId("lease-1")).thenReturn(deposit);

        assertThatThrownBy(() -> service.confirmSettlement(
                "admin-1", application.getId(),
                new LeaseTerminationDtos.SettlementConfirmRequest(
                        30000, 250000, null, "人工调整", List.of())))
                .hasMessageContaining("调整原因");
    }

    @Test
    void settlementMovesToRescissionWithoutEndingLeaseEarly() {
        LeaseTerminationApplication application = application("settlement_pending");
        application.setRefundAmount(0);
        Lease lease = activeLease();
        RentContract contract = signedContract();
        House house = new House();
        house.setId("house-1");
        house.setStatus("rented");
        when(applicationMapper.selectById(application.getId())).thenReturn(application);
        com.zhuxiang.service.entity.DepositRecord deposit = new com.zhuxiang.service.entity.DepositRecord();
        deposit.setId("deposit-1");
        deposit.setAmount(280000);
        deposit.setStatus("held");
        when(depositService.getByLeaseId("lease-1")).thenReturn(deposit);
        when(leaseService.getById("lease-1")).thenReturn(lease);
        when(contractMapper.selectById("contract-1")).thenReturn(contract);
        when(houseService.getById("house-1")).thenReturn(house);

        service.confirmSettlement(
                "admin-1",
                application.getId(),
                new LeaseTerminationDtos.SettlementConfirmRequest(
                        280000, 0, "线下协商押金全额抵扣", "扣除清洁费用", null)
        );

        assertThat(application.getStatus()).isEqualTo("rescission_pending");
        assertThat(application.getRefundAmount()).isZero();
        assertThat(application.getRefundAdjustmentReason()).isEqualTo("线下协商押金全额抵扣");
        assertThat(lease.getStatus()).isEqualTo("active");
        verify(depositService).refund("deposit-1");
        verify(depositService).settle(eq("deposit-1"), any(List.class), any(String.class));
        verify(leaseService, never()).updateById(any(Lease.class));
        verify(contractMapper, never()).updateById(any(RentContract.class));
        verify(houseService, never()).updateById(any(House.class));
    }

    @Test
    void rescissionUsesPlatformInitiatorWithoutTenantAuthorization() {
        LeaseTerminationApplication application = application("rescission_pending");
        RentContract contract = signedContract();
        contract.setSignFlowId("original-flow-1");
        contract.setContractFileId("file-1");
        when(applicationMapper.selectByIdForUpdate(application.getId())).thenReturn(application);
        when(esignProperties.isCredentialsConfigured()).thenReturn(true);
        when(contractMapper.selectById("contract-1")).thenReturn(contract);
        House platformHouse = new House();
        platformHouse.setId("house-1");
        platformHouse.setSourceType("PLATFORM");
        when(houseService.getById("house-1")).thenReturn(platformHouse);
        when(esignV3Client.initiatePlatformRescission(
                "original-flow-1", "file-1", "租赁关系终止：工作变动"))
                .thenReturn("rescission-flow-1");

        service.processPendingFlow(application.getId());

        assertThat(application.getStatus()).isEqualTo("rescission_signing");
        assertThat(application.getRescissionSignFlowId()).isEqualTo("rescission-flow-1");
        assertThat(application.getProcessLastError()).isNull();
        verify(esignV3Client, never()).hasPersonAuthorization(any(String.class), any(String.class));
        verify(esignV3Client, never()).createPersonAuthorizationUrl(any(String.class), any(String.class));
    }

    @Test
    void personalLandlordHouseRescissionDoesNotUsePlatformAutoSeal() {
        LeaseTerminationApplication application = application("rescission_pending");
        RentContract contract = signedContract();
        contract.setSignFlowId("original-flow-personal");
        contract.setContractFileId("file-personal");
        House landlordHouse = new House();
        landlordHouse.setId("house-1");
        landlordHouse.setSourceType("LANDLORD");
        when(applicationMapper.selectByIdForUpdate(application.getId())).thenReturn(application);
        when(esignProperties.isCredentialsConfigured()).thenReturn(true);
        when(contractMapper.selectById("contract-1")).thenReturn(contract);
        when(houseService.getById("house-1")).thenReturn(landlordHouse);
        when(esignV3Client.initiatePersonalHouseRescission(
                "original-flow-personal", "file-personal", "租赁关系终止：工作变动"))
                .thenReturn("rescission-flow-personal");

        service.processPendingFlow(application.getId());

        assertThat(application.getStatus()).isEqualTo("rescission_signing");
        assertThat(application.getRescissionSignFlowId()).isEqualTo("rescission-flow-personal");
        verify(esignV3Client, never()).initiatePlatformRescission(any(), any(), any());
    }

    @Test
    void permanentRescissionFailureStopsAutomaticRetry() {
        LeaseTerminationApplication application = application("rescission_pending");
        RentContract contract = signedContract();
        contract.setSignFlowId("original-flow-1");
        contract.setContractFileId("file-1");
        when(applicationMapper.selectByIdForUpdate(application.getId())).thenReturn(application);
        when(esignProperties.isCredentialsConfigured()).thenReturn(true);
        when(contractMapper.selectById("contract-1")).thenReturn(contract);
        when(esignV3Client.initiatePlatformRescission(any(), any(), any()))
                .thenThrow(EsignException.signingFailed(
                        "1439107", "发起方没有可以解约的文档，不允许解约", "/initiate-rescission"));

        service.processPendingFlow(application.getId());

        assertThat(application.getStatus()).isEqualTo("rescission_failed");
        assertThat(application.getRescissionStatus()).isEqualTo("failed");
        assertThat(application.getProcessRetryAt()).isNull();
        assertThat(application.getProcessRetryCount()).isEqualTo(1);
    }

    @Test
    void networkRescissionFailureUsesBackoffAndKeepsPending() {
        LeaseTerminationApplication application = application("rescission_pending");
        RentContract contract = signedContract();
        contract.setSignFlowId("original-flow-1");
        contract.setContractFileId("file-1");
        when(applicationMapper.selectByIdForUpdate(application.getId())).thenReturn(application);
        when(esignProperties.isCredentialsConfigured()).thenReturn(true);
        when(contractMapper.selectById("contract-1")).thenReturn(contract);
        when(esignV3Client.initiatePlatformRescission(any(), any(), any()))
                .thenThrow(new EsignException(
                        0, "NETWORK", "e签宝服务请求失败", "/initiate-rescission"));

        service.processPendingFlow(application.getId());

        assertThat(application.getStatus()).isEqualTo("rescission_pending");
        assertThat(application.getProcessRetryAt()).isNotNull();
        assertThat(application.getProcessRetryCount()).isEqualTo(1);
    }

    @Test
    void tenantCanGetRescissionSignUrlWhileSigning() {
        LeaseTerminationApplication application = application("rescission_signing");
        application.setRescissionSignFlowId("rescission-flow-1");
        User tenant = new User();
        tenant.setId("tenant-1");
        tenant.setPhone("13800138000");
        UserRealNameAuth auth = new UserRealNameAuth();
        auth.setUserId("tenant-1");
        auth.setAuthStatus("VERIFIED");
        auth.setAccountMobile("13800138000");
        EsignV3Client.SignUrlResponse response = new EsignV3Client.SignUrlResponse();
        EsignV3Client.SignUrlResponse.SignUrlData data =
                new EsignV3Client.SignUrlResponse.SignUrlData();
        data.setUrl("https://esign.example/sign");
        data.setShortUrl("https://esign.example/s/1");
        response.setData(data);

        when(applicationMapper.selectByIdForUpdate(application.getId())).thenReturn(application);
        when(userService.getById("tenant-1")).thenReturn(tenant);
        when(realNameAuthMapper.selectOne(any(Wrapper.class))).thenReturn(auth);
        when(esignV3Client.getRescissionSignUrl("rescission-flow-1", "13800138000"))
                .thenReturn(response);

        LeaseTerminationDtos.RescissionSignUrlResponse result =
                service.getRescissionSignUrl("tenant-1", application.getId());

        assertThat(result.signFlowId()).isEqualTo("rescission-flow-1");
        assertThat(result.action()).isEqualTo("sign");
        assertThat(result.signUrl()).isEqualTo("https://esign.example/sign");
    }

    @Test
    void pendingRescissionStartsAsPlatformAndReturnsTenantSignUrlWithoutAuthorization() {
        LeaseTerminationApplication application = application("rescission_pending");
        RentContract contract = signedContract();
        contract.setSignFlowId("original-flow-1");
        contract.setContractFileId("file-1");
        UserRealNameAuth auth = new UserRealNameAuth();
        auth.setId(10L);
        auth.setUserId("tenant-1");
        auth.setAuthStatus("VERIFIED");
        auth.setAccountMobile("13800138000");
        User tenant = new User();
        tenant.setId("tenant-1");
        tenant.setPhone("13800138000");
        EsignV3Client.SignUrlResponse response = new EsignV3Client.SignUrlResponse();
        EsignV3Client.SignUrlResponse.SignUrlData data =
                new EsignV3Client.SignUrlResponse.SignUrlData();
        data.setUrl("https://esign.example/rescission-sign");
        response.setData(data);

        when(applicationMapper.selectByIdForUpdate(application.getId())).thenReturn(application);
        when(contractMapper.selectById("contract-1")).thenReturn(contract);
        when(realNameAuthMapper.selectOne(any(Wrapper.class))).thenReturn(auth);
        when(userService.getById("tenant-1")).thenReturn(tenant);
        when(esignV3Client.initiatePlatformRescission(
                "original-flow-1", "file-1", "租赁关系终止：工作变动"))
                .thenReturn("rescission-flow-2");
        when(esignV3Client.getRescissionSignUrl("rescission-flow-2", "13800138000"))
                .thenReturn(response);

        LeaseTerminationDtos.RescissionSignUrlResponse result =
                service.getRescissionSignUrl("tenant-1", application.getId());

        assertThat(result.action()).isEqualTo("sign");
        assertThat(result.signFlowId()).isEqualTo("rescission-flow-2");
        assertThat(result.signUrl()).isEqualTo("https://esign.example/rescission-sign");
        verify(esignV3Client, never()).hasPersonAuthorization(any(String.class), any(String.class));
        verify(esignV3Client, never()).createPersonAuthorizationUrl(any(String.class), any(String.class));
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
