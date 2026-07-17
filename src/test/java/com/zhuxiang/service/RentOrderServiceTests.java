package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.dto.CreateRentOrderRequest;
import com.zhuxiang.service.dto.RentOrderResponse;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.entity.UserRealNameAuth;
import com.zhuxiang.service.mapper.*;
import com.zhuxiang.service.service.*;
import com.zhuxiang.service.service.impl.RentOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RentOrderServiceTests {

    private static final String TEST_USER_ID = UUID.randomUUID().toString();
    private static final String TEST_HOUSE_ID = "house_test_001";

    private final RentOrderMapper rentOrderMapper = mock(RentOrderMapper.class);
    private final HouseService houseService = mock(HouseService.class);
    private final RentContractMapper rentContractMapper = mock(RentContractMapper.class);
    private final LeaseService leaseService = mock(LeaseService.class);
    private final LandlordService landlordService = mock(LandlordService.class);
    private final FileRecordService fileRecordService = mock(FileRecordService.class);
    private final PaymentRecordService paymentRecordService = mock(PaymentRecordService.class);
    private final RentBillService rentBillService = mock(RentBillService.class);
    private final AlipayService alipayService = mock(AlipayService.class);
    private final DepositService depositService = mock(DepositService.class);
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();
    private final RealNameAuthService realNameAuthService = mock(RealNameAuthService.class);
    private final UserRealNameAuthMapper userRealNameAuthMapper = mock(UserRealNameAuthMapper.class);
    private final IdCardCryptoService idCardCryptoService = mock(IdCardCryptoService.class);
    private final org.springframework.context.ApplicationEventPublisher eventPublisher =
            mock(org.springframework.context.ApplicationEventPublisher.class);
    private final EsignV3Client esignV3Client = mock(EsignV3Client.class);
    private final EsignV3Properties esignV3Properties = mock(EsignV3Properties.class);
    private final InspectionService inspectionService = mock(InspectionService.class);

    private RentOrderServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = spy(new RentOrderServiceImpl(
                houseService, rentContractMapper, leaseService, landlordService,
                eventPublisher, fileRecordService, paymentRecordService,
                rentBillService, alipayService, depositService, objectMapper,
                realNameAuthService, userRealNameAuthMapper, idCardCryptoService,
                esignV3Client, esignV3Properties, inspectionService
        ));
        ReflectionTestUtils.setField(service, "baseMapper", rentOrderMapper);

        // 默认：已实名
        when(realNameAuthService.isVerified(TEST_USER_ID)).thenReturn(true);
        UserRealNameAuth verifiedAuth = buildVerifiedAuth();
        when(userRealNameAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(verifiedAuth);
        when(idCardCryptoService.decrypt(anyString())).thenReturn("110101199001010000");
        when(idCardCryptoService.mask(anyString())).thenReturn("110101********0000");

        House house = buildHouse();
        when(houseService.getById(TEST_HOUSE_ID)).thenReturn(house);

        // lambdaUpdate 链 mock：方法链返回自身，update() 返回 true
        @SuppressWarnings("unchecked")
        LambdaUpdateChainWrapper<House> updateChain = mock(LambdaUpdateChainWrapper.class, invocation -> {
            if ("update".equals(invocation.getMethod().getName())) {
                return true;
            }
            return invocation.getMock();
        });
        when(houseService.lambdaUpdate()).thenReturn(updateChain);
    }

    // ==================== 未实名 → REAL_NAME_REQUIRED ====================

    @Test
    void createOrder_shouldThrowRealNameRequiredWhenNotVerified() {
        when(realNameAuthService.isVerified(TEST_USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.createOrder(TEST_USER_ID, buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先完成实名认证");
    }

    @Test
    void createOrder_shouldThrowRealNameRequiredWhenNoAuthRecord() {
        when(userRealNameAuthMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.createOrder(TEST_USER_ID, buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先完成实名认证");
    }

    // ==================== VERIFIED → pendingContract ====================

    @Test
    void createOrder_shouldSetStatusToPendingContract() {
        setupNewOrderSuccess();

        RentOrderResponse result = service.createOrder(TEST_USER_ID, buildRequest());

        assertThat(result.status()).isEqualTo("pendingContract");
    }

    @Test
    void createOrder_shouldReturnValidOrderId() {
        setupNewOrderSuccess();

        RentOrderResponse result = service.createOrder(TEST_USER_ID, buildRequest());

        assertThat(result.id()).isNotNull().isNotEmpty();
    }

    @Test
    void createOrder_shouldGenerateContract() {
        setupNewOrderSuccess();

        service.createOrder(TEST_USER_ID, buildRequest());

        verify(rentContractMapper).insert(any(RentContract.class));
    }

    @Test
    void createOrder_shouldUseFullIdCardInContract() {
        setupNewOrderSuccess();
        when(idCardCryptoService.decrypt(anyString())).thenReturn("110101199001010000");

        service.createOrder(TEST_USER_ID, buildRequest());

        verify(rentContractMapper).insert(argThat((RentContract c) ->
                "110101199001010000".equals(c.getTenantIdCard())));
    }

    @Test
    void createOrder_shouldMaskIdCardInResponse() {
        setupNewOrderSuccess();
        when(idCardCryptoService.mask("110101199001010000")).thenReturn("110101********0000");

        RentOrderResponse result = service.createOrder(TEST_USER_ID, buildRequest());

        assertThat(result.tenantIdCard()).isEqualTo("110101********0000");
        assertThat(result.tenantIdCard()).doesNotContain("19900101");
    }

    // ==================== pendingRealName 自动升级 ====================

    @Test
    void createOrder_shouldUpgradePendingRealNameToPendingContract() {
        RentOrder existing = buildPendingRealNameOrder();
        when(rentOrderMapper.selectOne(any())).thenReturn(existing);
        when(rentContractMapper.selectCount(any())).thenReturn(0L);

        RentOrderResponse result = service.createOrder(TEST_USER_ID, buildRequest());

        assertThat(result.status()).isEqualTo("pendingContract");
        assertThat(result.id()).isEqualTo(existing.getId());
    }

    @Test
    void createOrder_shouldNotDuplicateContractWhenUpgrading() {
        RentOrder existing = buildPendingRealNameOrder();
        when(rentOrderMapper.selectOne(any())).thenReturn(existing);
        when(rentContractMapper.selectCount(any())).thenReturn(1L);

        service.createOrder(TEST_USER_ID, buildRequest());

        verify(rentContractMapper, never()).insert(any(RentContract.class));
    }

    // ==================== 已有订单：当前用户 ====================

    @Test
    void createOrder_shouldUpgradeCreatedToPendingContract() {
        RentOrder existing = buildPendingRealNameOrder();
        existing.setStatus("created");
        when(rentOrderMapper.selectOne(any())).thenReturn(existing);
        when(rentContractMapper.selectCount(any())).thenReturn(0L);

        RentOrderResponse result = service.createOrder(TEST_USER_ID, buildRequest());

        assertThat(result.status()).isEqualTo("pendingContract");
        assertThat(result.id()).isEqualTo(existing.getId());
    }

    @Test
    void createOrder_shouldReturnExistingPendingContract() {
        RentOrder existing = buildPendingRealNameOrder();
        existing.setStatus("pendingContract");
        when(rentOrderMapper.selectOne(any())).thenReturn(existing);

        RentOrderResponse result = service.createOrder(TEST_USER_ID, buildRequest());

        assertThat(result.status()).isEqualTo("pendingContract");
        assertThat(result.id()).isEqualTo(existing.getId());
        // 不创建新订单，不生成合同
        verify(rentContractMapper, never()).insert(any(RentContract.class));
    }

    @Test
    void createOrder_shouldReturnExistingPendingPayment() {
        RentOrder existing = buildPendingRealNameOrder();
        existing.setStatus("pendingPayment");
        when(rentOrderMapper.selectOne(any())).thenReturn(existing);

        RentOrderResponse result = service.createOrder(TEST_USER_ID, buildRequest());

        assertThat(result.status()).isEqualTo("pendingPayment");
        assertThat(result.id()).isEqualTo(existing.getId());
    }

    @Test
    void createOrder_shouldReturnExistingPendingSign() {
        RentOrder existing = buildPendingRealNameOrder();
        existing.setStatus("pendingSign");
        when(rentOrderMapper.selectOne(any())).thenReturn(existing);

        RentOrderResponse result = service.createOrder(TEST_USER_ID, buildRequest());

        assertThat(result.status()).isEqualTo("pendingSign");
        assertThat(result.id()).isEqualTo(existing.getId());
    }

    @Test
    void createOrder_shouldRejectWhenCompleted() {
        RentOrder existing = buildPendingRealNameOrder();
        existing.setStatus("completed");
        when(rentOrderMapper.selectOne(any())).thenReturn(existing);

        assertThatThrownBy(() -> service.createOrder(TEST_USER_ID, buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已完成租住");
    }

    // ==================== 已有订单：其他用户 ====================

    @Test
    void createOrder_shouldRejectWhenOtherUserHasPending() {
        // 当前用户无进行中订单（FOR UPDATE → null）
        when(rentOrderMapper.selectOne(any())).thenReturn(null);
        // 其他用户有进行中订单 → count 返回 1
        doReturn(1L).when(service).count(any());

        assertThatThrownBy(() -> service.createOrder(TEST_USER_ID, buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有租客办理中");
    }

    // ==================== 合同生成失败 ====================

    @Test
    void createOrder_shouldThrowWhenContractInsertFails() {
        setupNewOrderSuccess();
        doThrow(new RuntimeException("DB error")).when(rentContractMapper).insert(any(RentContract.class));

        assertThatThrownBy(() -> service.createOrder(TEST_USER_ID, buildRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");
    }

    // ==================== helpers ====================

    private void setupNewOrderSuccess() {
        // rentOrderMapper.selectOne → null（无 pendingRealName）
        when(rentOrderMapper.selectOne(any())).thenReturn(null);
        // count(进行中订单) → 0
        doReturn(0L).when(service).count(any());
        // 活跃租约检查
        when(leaseService.count(any())).thenReturn(0L);
    }

    private CreateRentOrderRequest buildRequest() {
        return new CreateRentOrderRequest(
                TEST_HOUSE_ID,
                LocalDate.now().plusDays(1),
                12,
                "monthly",
                2
        );
    }

    private UserRealNameAuth buildVerifiedAuth() {
        UserRealNameAuth auth = new UserRealNameAuth();
        auth.setId(1L);
        auth.setUserId(TEST_USER_ID);
        auth.setRealNameAuthNo("RNA-TEST-VERIFIED");
        auth.setAuthStatus("VERIFIED");
        auth.setRealName("测试用户");
        auth.setAccountMobile("13800138000");
        auth.setIdCardCiphertext("v1:mockCiphertext");
        auth.setIdCardMasked("110101********0000");
        return auth;
    }

    private RentOrder buildPendingRealNameOrder() {
        RentOrder order = new RentOrder();
        order.setId("existing-pending-id");
        order.setUserId(TEST_USER_ID);
        order.setHouseId(TEST_HOUSE_ID);
        order.setStatus("pendingRealName");
        order.setStartDate(LocalDate.now().plusDays(1));
        order.setEndDate(LocalDate.now().plusMonths(12));
        order.setLeaseMonths(12);
        order.setPaymentMethod("monthly");
        order.setPaymentMonths(1);
        order.setTenantCount(2);
        order.setMonthlyRent(200000);
        order.setDeposit(200000);
        order.setServiceFee(20000);
        order.setFirstPaymentAmount(420000);
        order.setTotalAmount(2620000);
        return order;
    }

    private House buildHouse() {
        House house = new House();
        house.setId(TEST_HOUSE_ID);
        house.setTitle("测试房源");
        house.setBuilding("1");
        house.setUnit("2");
        house.setRoom("301");
        house.setPrice(200000);
        house.setDeposit(200000);
        house.setStatus("available");
        house.setAddress("测试地址");
        return house;
    }
}
