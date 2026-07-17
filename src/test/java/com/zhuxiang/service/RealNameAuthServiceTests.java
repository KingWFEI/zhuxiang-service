package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zhuxiang.service.client.EsignFaceAuthClient;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.EsignApiException;
import com.zhuxiang.service.config.EsignFaceProperties;
import com.zhuxiang.service.dto.EsignFaceAuthCreateResponse;
import com.zhuxiang.service.dto.EsignFaceAuthDetailResponse;
import com.zhuxiang.service.dto.RealNameAuthDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.entity.UserRealNameAuth;
import com.zhuxiang.service.enums.RealNameAuthStatus;
import com.zhuxiang.service.mapper.UserRealNameAuthMapper;
import com.zhuxiang.service.service.IdCardCryptoService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.RealNameAuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 个人实名认证业务服务单元测试。
 * <p>
 * 使用 Mockito 模拟所有外部依赖（Mapper、Client、加密服务等）。
 * 所有测试数据均为虚构，不使用真实姓名、身份证号、AppId/AppSecret/AES 密钥。
 * <p>
 * 直接调用 service 方法（不通过 AOP 代理），内部 @Transactional 不会触发，
 * 但业务逻辑流完整覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RealNameAuthServiceTests {

    private static final String TEST_USER_ID = "test-user-uuid-12345";
    private static final String TEST_PHONE = "13800138000";
    private static final String TEST_REAL_NAME = "测试用户";
    private static final String TEST_ID_CARD = "110101199001010000";
    private static final String TEST_ID_CARD_TYPE = "INDIVIDUAL_CH_IDCARD";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String TEST_AUTH_NO = "RNA-TEST-001";

    @Mock
    private UserRealNameAuthMapper mapper;
    @Mock
    private UserService userService;
    @Mock
    private IdCardCryptoService cryptoService;
    @Mock
    private EsignFaceAuthClient esignClient;
    @Mock
    private EsignFaceProperties esignProperties;

    private RealNameAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        when(esignProperties.getMode()).thenReturn("ESIGN");
        when(esignProperties.getCallbackUrl()).thenReturn("https://example.com/callback");
        when(cryptoService.encrypt(anyString())).thenReturn("v1:mockCiphertext");
        when(cryptoService.decrypt(anyString())).thenReturn(TEST_ID_CARD);
        when(cryptoService.mask(anyString())).thenReturn("110101********0000");

        // 模拟 insert 后设置主键 ID (ID 递增)
        doAnswer(inv -> {
            UserRealNameAuth entity = inv.getArgument(0);
            entity.setId(100L);
            return 1;
        }).when(mapper).insert(any(UserRealNameAuth.class));

        // 默认 update 返回 1
        when(mapper.update(any(), any())).thenReturn(1);

        // 默认 selectById 返回 null，具体测试按需覆盖
        when(mapper.selectById(any())).thenReturn(null);

        service = new RealNameAuthServiceImpl(mapper, userService, cryptoService, esignClient, esignProperties);
        service.setSelf(service);
    }

    // ==================== getStatus ====================

    @Test
    void getStatus_shouldReturnUnverifiedForNoRecord() {
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.UNVERIFIED.getValue());
        assertThat(result.realNameAuthNo()).isNull();
    }

    @Test
    void getStatus_shouldAutoExpireWhenAuthUrlExpired() {
        UserRealNameAuth verifying = buildVerifyingAuth();
        verifying.setAuthUrlExpireTime(LocalDateTime.now(ZONE).minusHours(1)); // 已过期
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(verifying);

        // 自调用 updateStatusIfVerifying → EXPIRED
        when(mapper.update(any(), any())).thenReturn(1);
        // 第二次 selectById 返回更新后的
        when(mapper.selectById(1L)).thenReturn(buildExpiredAuth());

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.EXPIRED.getValue());
        assertThat(result.authUrlExpireTime()).isNull(); // 过期不返回 authUrlExpireTime
        assertThat(result.authUrl()).isNull(); // 过期不返回 authUrl
    }

    @Test
    void getStatus_shouldReturnVerifyingWhenAuthUrlNotExpired() {
        UserRealNameAuth verifying = buildVerifyingAuth();
        verifying.setAuthUrl("https://mock.auth.url/flow/test");
        verifying.setAuthUrlExpireTime(LocalDateTime.now(ZONE).plusHours(1)); // 未过期
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(verifying);

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
        assertThat(result.authUrlExpireTime()).isNotNull(); // 有效才返回
        assertThat(result.authUrl()).isEqualTo("https://mock.auth.url/flow/test"); // 有效才返回
    }

    @Test
    void getStatus_shouldMaskSensitiveInfo() {
        UserRealNameAuth auth = buildVerifyingAuth();
        auth.setAccountMobile("13800138000");
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        assertThat(result.idCardMasked()).isEqualTo("110101********0000");
        assertThat(result.realNameMasked()).isEqualTo("测***");
        assertThat(result.accountMobileMasked()).isEqualTo("138****8000");
    }

    @Test
    void getStatus_shouldReturnAuthUrlWhenVerifyingAndValid() {
        // 用户重新进入 APP，VERIFYING 状态且 authUrl 有效 → 返回 authUrl 以便重新打开认证网页
        UserRealNameAuth verifying = buildVerifyingAuth();
        verifying.setAuthUrl("https://mock.auth.url/flow/abc123");
        verifying.setAuthUrlExpireTime(LocalDateTime.now(ZONE).plusHours(1));
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(verifying);

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
        assertThat(result.authUrl()).isEqualTo("https://mock.auth.url/flow/abc123");
        assertThat(result.authUrlExpireTime()).isNotNull();
    }

    @Test
    void getStatus_shouldNotReturnAuthUrlWhenExpired() {
        UserRealNameAuth verifying = buildVerifyingAuth();
        verifying.setAuthUrl("https://expired.auth.url");
        verifying.setAuthUrlExpireTime(LocalDateTime.now(ZONE).minusHours(1)); // 已过期
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(verifying);

        when(mapper.update(any(), any())).thenReturn(1);
        when(mapper.selectById(1L)).thenReturn(buildExpiredAuth());

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.EXPIRED.getValue());
        assertThat(result.authUrl()).isNull(); // 过期不返回 authUrl
        assertThat(result.authUrlExpireTime()).isNull();
    }

    @Test
    void getStatus_shouldNotReturnAuthUrlWhenVerified() {
        UserRealNameAuth verified = buildVerifiedAuth();
        verified.setAuthUrl(null); // VERIFIED 应该没有 authUrl
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(verified);

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFIED.getValue());
        assertThat(result.authUrl()).isNull();
        assertThat(result.authUrlExpireTime()).isNull();
    }

    @Test
    void getStatus_shouldNotReturnAuthUrlWhenFailed() {
        UserRealNameAuth failed = buildVerifyingAuth();
        failed.setAuthStatus(RealNameAuthStatus.FAILED.getValue());
        failed.setAuthUrl("https://stale.auth.url");
        failed.setAuthUrlExpireTime(LocalDateTime.now(ZONE).plusHours(1));
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(failed);

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.FAILED.getValue());
        assertThat(result.authUrl()).isNull(); // FAILED 不返回 authUrl
        assertThat(result.authUrlExpireTime()).isNull();
    }

    @Test
    void getStatus_shouldNotReturnAuthUrlWhenCancelled() {
        UserRealNameAuth cancelled = buildVerifyingAuth();
        cancelled.setAuthStatus(RealNameAuthStatus.CANCELED.getValue());
        cancelled.setAuthUrl("https://stale.auth.url");
        cancelled.setAuthUrlExpireTime(LocalDateTime.now(ZONE).plusHours(1));
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cancelled);

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.CANCELED.getValue());
        assertThat(result.authUrl()).isNull();
    }

    @Test
    void getStatus_shouldNotReturnIdCardPlaintextOrCiphertext() {
        UserRealNameAuth auth = buildVerifyingAuth();
        auth.setIdCardCiphertext("v1:TOP_SECRET_CIPHERTEXT_DO_NOT_LEAK");
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);

        // StatusResult record 没有 idCardCiphertext 字段 → 编译级安全
        assertThat(result.idCardMasked()).doesNotContain("11010119900101");
    }

    // ==================== startAuth ====================

    @Test
    void startAuth_shouldSucceedForNewUser() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // checkAndCreateVerifying: selectOne FOR UPDATE → null (no existing)
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        EsignFaceAuthCreateResponse esignResp = buildCreateResponse();
        when(esignClient.createFaceAuth(any())).thenReturn(esignResp);
        when(mapper.selectById(100L)).thenReturn(buildResultAuth());

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        RealNameAuthDtos.StartResult result = service.startAuth(TEST_USER_ID, req);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
        assertThat(result.realNameAuthNo()).isNotNull();
        assertThat(result.idCardMasked()).isEqualTo("110101********0000");
    }

    @Test
    void startAuth_shouldRejectAlreadyVerifiedUser() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        assertThatThrownBy(() -> service.startAuth(TEST_USER_ID, req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409)
                .hasMessageContaining("已完成实名认证");
    }

    @Test
    void startAuth_shouldThrow409WhenVerifyingNotExpired() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // checkAndCreateVerifying: 存在有效 VERIFYING 记录
        UserRealNameAuth existing = new UserRealNameAuth();
        existing.setId(10L);
        existing.setUserId(TEST_USER_ID);
        existing.setRealNameAuthNo("RNA-TEST-EXISTING");
        existing.setAuthStatus(RealNameAuthStatus.VERIFYING.getValue());
        existing.setAuthUrl("https://valid.auth.url");
        existing.setAuthUrlExpireTime(LocalDateTime.now(ZONE).plusHours(1));

        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        assertThatThrownBy(() -> service.startAuth(TEST_USER_ID, req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409)
                .hasMessageContaining("进行中的认证任务");

        verify(esignClient, never()).createFaceAuth(any());
    }

    @Test
    void startAuth_shouldExpireOldVerifyingAndCreateNew() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // checkAndCreateVerifying: 存在已过期 VERIFYING 记录
        UserRealNameAuth expired = new UserRealNameAuth();
        expired.setId(11L);
        expired.setUserId(TEST_USER_ID);
        expired.setRealNameAuthNo("RNA-TEST-EXPIRED");
        expired.setAuthStatus(RealNameAuthStatus.VERIFYING.getValue());
        expired.setAuthUrl("https://expired.auth.url");
        expired.setAuthUrlExpireTime(LocalDateTime.now(ZONE).minusHours(1)); // 已过期

        // selectOne 第一次返回过期记录（checkAndCreateVerifying），更新后第二次调用 insert 时用不到
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(expired);

        EsignFaceAuthCreateResponse esignResp = buildCreateResponse();
        when(esignClient.createFaceAuth(any())).thenReturn(esignResp);
        when(mapper.selectById(100L)).thenReturn(buildResultAuth());

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        RealNameAuthDtos.StartResult result = service.startAuth(TEST_USER_ID, req);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
    }

    @Test
    void startAuth_shouldUpdateFailedWhenEsignFails() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(mapper.insert(any(UserRealNameAuth.class))).thenReturn(1);
        when(mapper.update(any(), any())).thenReturn(1);

        when(esignClient.createFaceAuth(any())).thenThrow(
                new EsignApiException(200, 30503107, "人脸实名认证服务余额不足", "/v2/identity/auth/api/individual/face"));

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        assertThatThrownBy(() -> service.startAuth(TEST_USER_ID, req))
                .isInstanceOf(EsignApiException.class);
    }

    @Test
    void startAuth_shouldAllowRetryAfterFailed() {
        // FAILED 状态的用户可以重新发起认证
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L); // isVerified → false
        // checkAndCreateVerifying: 没有 VERIFYING → 创建新记录
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        EsignFaceAuthCreateResponse esignResp = buildCreateResponse();
        when(esignClient.createFaceAuth(any())).thenReturn(esignResp);
        when(mapper.selectById(100L)).thenReturn(buildResultAuth());

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        RealNameAuthDtos.StartResult result = service.startAuth(TEST_USER_ID, req);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
        // 确认调用了 e签宝
        verify(esignClient).createFaceAuth(any());
    }

    // ==================== refreshAuth ====================

    @Test
    void refreshAuth_shouldUpdateToVerifiedWhenAllMatches() {
        UserRealNameAuth auth = buildVerifyingAuth();
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        EsignFaceAuthDetailResponse detail = buildSuccessDetail();
        when(esignClient.queryFaceAuthDetail("4512345678901565")).thenReturn(detail);
        when(mapper.update(any(), any())).thenReturn(1);
        when(mapper.selectById(1L)).thenReturn(buildVerifiedAuth());

        RealNameAuthDtos.RefreshResult result = service.refreshAuth(TEST_USER_ID, TEST_AUTH_NO);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFIED.getValue());
        assertThat(result.authUrl()).isNull();
    }

    @Test
    void refreshAuth_shouldReturnVerifiedDirectlyWhenAlreadyVerified() {
        UserRealNameAuth verified = buildVerifyingAuth();
        verified.setAuthStatus(RealNameAuthStatus.VERIFIED.getValue());
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(verified);

        RealNameAuthDtos.RefreshResult result = service.refreshAuth(TEST_USER_ID, TEST_AUTH_NO);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFIED.getValue());
        assertThat(result.authUrl()).isNull();
        verify(esignClient, never()).queryFaceAuthDetail(anyString());
    }

    @Test
    void refreshAuth_shouldExpireWhenAuthUrlExpired() {
        UserRealNameAuth verifying = buildVerifyingAuth();
        verifying.setAuthUrlExpireTime(LocalDateTime.now(ZONE).minusHours(1)); // 已过期
        verifying.setAuthUrl("https://expired.url");
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(verifying);

        // updateStatusIfVerifying → EXPIRED
        when(mapper.update(any(), any())).thenReturn(1);
        // 第二次 selectById 返回更新后
        UserRealNameAuth expired = buildVerifyingAuth();
        expired.setAuthStatus(RealNameAuthStatus.EXPIRED.getValue());
        expired.setAuthUrlExpireTime(LocalDateTime.now(ZONE).minusHours(1));
        when(mapper.selectById(1L)).thenReturn(expired);

        RealNameAuthDtos.RefreshResult result = service.refreshAuth(TEST_USER_ID, TEST_AUTH_NO);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.EXPIRED.getValue());
        assertThat(result.authUrl()).isNull();
        verify(esignClient, never()).queryFaceAuthDetail(anyString());
    }

    @Test
    void refreshAuth_shouldRejectWhenNameMismatch() {
        UserRealNameAuth auth = buildVerifyingAuth();
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        EsignFaceAuthDetailResponse detail = buildSuccessDetail();
        detail.getData().getIndivInfo().setName("其他人");
        when(esignClient.queryFaceAuthDetail("4512345678901565")).thenReturn(detail);
        when(mapper.update(any(), any())).thenReturn(1);

        assertThatThrownBy(() -> service.refreshAuth(TEST_USER_ID, TEST_AUTH_NO))
                .isInstanceOf(EsignApiException.class)
                .hasMessageContaining("不一致");
    }

    @Test
    void refreshAuth_shouldRejectWhenCertTypeMismatch() {
        UserRealNameAuth auth = buildVerifyingAuth();
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        EsignFaceAuthDetailResponse detail = buildSuccessDetail();
        detail.getData().getIndivInfo().setCertType("PASSPORT");
        when(esignClient.queryFaceAuthDetail("4512345678901565")).thenReturn(detail);
        when(mapper.update(any(), any())).thenReturn(1);

        assertThatThrownBy(() -> service.refreshAuth(TEST_USER_ID, TEST_AUTH_NO))
                .isInstanceOf(EsignApiException.class)
                .hasMessageContaining("不一致");
    }

    @Test
    void refreshAuth_shouldRejectWhenCertNoMismatch() {
        when(cryptoService.decrypt("v1:mockCiphertext")).thenReturn("110101199001010000");
        UserRealNameAuth auth = buildVerifyingAuth();
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        EsignFaceAuthDetailResponse detail = buildSuccessDetail();
        detail.getData().getIndivInfo().setCertNo("110101199001019999");
        when(esignClient.queryFaceAuthDetail("4512345678901565")).thenReturn(detail);
        when(mapper.update(any(), any())).thenReturn(1);

        assertThatThrownBy(() -> service.refreshAuth(TEST_USER_ID, TEST_AUTH_NO))
                .isInstanceOf(EsignApiException.class)
                .hasMessageContaining("不一致");
    }

    @Test
    void refreshAuth_shouldStayVerifyingForUnknownStatus() {
        UserRealNameAuth auth = buildVerifyingAuth();
        auth.setAuthUrl("https://mock.auth.url/e-sign-ing");
        auth.setAuthUrlExpireTime(LocalDateTime.now(ZONE).plusHours(1));
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        EsignFaceAuthDetailResponse detail = buildSuccessDetail();
        detail.getData().setStatus("PROCESSING");
        detail.getData().setFailReason(null);
        when(esignClient.queryFaceAuthDetail("4512345678901565")).thenReturn(detail);
        when(mapper.selectById(1L)).thenReturn(auth);

        RealNameAuthDtos.RefreshResult result = service.refreshAuth(TEST_USER_ID, TEST_AUTH_NO);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
        assertThat(result.authUrl()).isEqualTo("https://mock.auth.url/e-sign-ing");
        assertThat(result.authUrlExpireTime()).isNotNull();
    }

    @Test
    void refreshAuth_shouldRejectOtherUsersRecord() {
        UserRealNameAuth auth = buildVerifyingAuth();
        auth.setUserId("other-user-id");
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        assertThatThrownBy(() -> service.refreshAuth(TEST_USER_ID, TEST_AUTH_NO))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 403)
                .hasMessageContaining("无权操作");
    }

    @Test
    void refreshAuth_shouldReturn404WhenRecordNotFound() {
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.refreshAuth(TEST_USER_ID, "RNA-NONEXISTENT"))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 404);
    }

    @Test
    void refreshAuth_shouldHandleEsignQueryExceptionGracefully() {
        UserRealNameAuth auth = buildVerifyingAuth();
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        when(esignClient.queryFaceAuthDetail(anyString()))
                .thenThrow(new EsignApiException(200, 30503100, "认证记录不存在",
                        "/v2/identity/auth/api/common/4512345678901565/detail"));

        // 不应抛异常，返回当前状态 + 保留原 authUrl
        RealNameAuthDtos.RefreshResult result = service.refreshAuth(TEST_USER_ID, TEST_AUTH_NO);
        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
        assertThat(result.authUrl()).isEqualTo("https://mock.auth.url");
    }

    // ==================== restartAuth ====================

    @Test
    void restartAuth_shouldExpireOldVerifyingAndCreateNew() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // forceExpireAndCreate: 第一次 lockVerifying → 存在 VERIFYING
        // expireVerifying 更新后，第二次 lockVerifying → null（无 VERIFYING）
        UserRealNameAuth old = buildVerifyingAuth();
        old.setRealNameAuthNo("RNA-OLD-EXPIRED");
        when(mapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(old)   // 第一次 lockVerifying
                .thenReturn(null); // 第二次 lockVerifying（过期后）

        EsignFaceAuthCreateResponse esignResp = buildCreateResponse();
        when(esignClient.createFaceAuth(any())).thenReturn(esignResp);
        when(mapper.selectById(100L)).thenReturn(buildResultAuth());

        RealNameAuthDtos.RestartRequest req = new RealNameAuthDtos.RestartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        RealNameAuthDtos.StartResult result = service.restartAuth(TEST_USER_ID, req);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
        verify(esignClient).createFaceAuth(any());
    }

    @Test
    void restartAuth_shouldSucceedWhenNoExistingVerifying() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 两次 lockVerifying 都返回 null
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        EsignFaceAuthCreateResponse esignResp = buildCreateResponse();
        when(esignClient.createFaceAuth(any())).thenReturn(esignResp);
        when(mapper.selectById(100L)).thenReturn(buildResultAuth());

        RealNameAuthDtos.RestartRequest req = new RealNameAuthDtos.RestartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        RealNameAuthDtos.StartResult result = service.restartAuth(TEST_USER_ID, req);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
    }

    @Test
    void restartAuth_shouldRejectVerifiedUser() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L); // isVerified → true

        RealNameAuthDtos.RestartRequest req = new RealNameAuthDtos.RestartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        assertThatThrownBy(() -> service.restartAuth(TEST_USER_ID, req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409);
    }

    @Test
    void restartAuth_shouldReturnNewAuthUrl() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 第一次 lockVerifying → 存在旧 VERIFYING（会被过期）
        // 第二次 lockVerifying → null
        UserRealNameAuth old = buildVerifyingAuth();
        old.setAuthUrl("https://old-stale.url");
        when(mapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(old).thenReturn(null);

        EsignFaceAuthCreateResponse esignResp = buildCreateResponse();
        esignResp.getData().setAuthUrl("https://new-auth.url");
        when(esignClient.createFaceAuth(any())).thenReturn(esignResp);

        UserRealNameAuth resultAuth = buildResultAuth();
        resultAuth.setAuthUrl("https://new-auth.url");
        when(mapper.selectById(100L)).thenReturn(resultAuth);

        RealNameAuthDtos.RestartRequest req = new RealNameAuthDtos.RestartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        RealNameAuthDtos.StartResult result = service.restartAuth(TEST_USER_ID, req);

        assertThat(result.authUrl()).isEqualTo("https://new-auth.url");
    }

    // ==================== start 查询 e签宝后重试 ====================

    @Test
    void startAuth_shouldQueryEsignAndRetryWhenProviderFailed() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // findLatestVerifying → 存在 VERIFYING 且有 flowId
        UserRealNameAuth stale = buildVerifyingAuth();
        stale.setEsignFaceFlowId("4512345678901565");
        stale.setAuthUrlExpireTime(LocalDateTime.now(ZONE).minusHours(1)); // 已过期

        // resolveStaleVerifying 中 selectOne，然后 updateStatusIfVerifying → FAILED
        // checkAndCreateVerifying 中 lockVerifying → null（旧记录已变 FAILED）
        when(mapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(stale)  // findLatestVerifying
                .thenReturn(null);  // lockVerifying（checkAndCreateVerifying）

        // e签宝查询 → FAILED
        EsignFaceAuthDetailResponse failDetail = buildSuccessDetail();
        failDetail.getData().setStatus("FAILED");
        when(esignClient.queryFaceAuthDetail("4512345678901565")).thenReturn(failDetail);

        EsignFaceAuthCreateResponse esignResp = buildCreateResponse();
        when(esignClient.createFaceAuth(any())).thenReturn(esignResp);
        when(mapper.selectById(100L)).thenReturn(buildResultAuth());

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        RealNameAuthDtos.StartResult result = service.startAuth(TEST_USER_ID, req);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
        verify(esignClient).queryFaceAuthDetail("4512345678901565");
        verify(esignClient).createFaceAuth(any());
    }

    @Test
    void startAuth_shouldQueryEsignAndReject409WhenProviderIngAndUrlValid() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // findLatestVerifying → VERIFYING + flowId + authUrl 有效
        UserRealNameAuth ing = buildVerifyingAuth();
        ing.setEsignFaceFlowId("4512345678901565");
        ing.setAuthUrl("https://valid.url");
        ing.setAuthUrlExpireTime(LocalDateTime.now(ZONE).plusHours(1)); // 未过期

        // resolveStaleVerifying 中 selectOne → ing
        // checkAndCreateVerifying 中 lockVerifying → ing（仍在 VERIFYING 且有效）
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ing);

        // e签宝查询 → ING
        EsignFaceAuthDetailResponse ingDetail = buildSuccessDetail();
        ingDetail.getData().setStatus("PROCESSING");
        when(esignClient.queryFaceAuthDetail("4512345678901565")).thenReturn(ingDetail);

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        assertThatThrownBy(() -> service.startAuth(TEST_USER_ID, req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409);
    }

    @Test
    void startAuth_shouldQueryEsignAndRetryWhenUrlExpired() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // findLatestVerifying → VERIFYING + flowId + authUrl 过期
        UserRealNameAuth stale = buildVerifyingAuth();
        stale.setEsignFaceFlowId("4512345678901565");
        stale.setAuthUrlExpireTime(LocalDateTime.now(ZONE).minusHours(1));

        // resolveStaleVerifying: selectOne → stale, e签宝 → PROCESSING
        // 映射为 VERIFYING，但 authUrl 已过期，resolveStaleVerifying 不更新
        // checkAndCreateVerifying: lockVerifying → stale, isAuthUrlValid → false, expire → 插入新记录
        when(mapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(stale)  // findLatestVerifying
                .thenReturn(stale)  // lockVerifying → 过期
                .thenReturn(null);  // 实际上不需要第三次，但安全

        EsignFaceAuthDetailResponse ingDetail = buildSuccessDetail();
        ingDetail.getData().setStatus("ING");
        when(esignClient.queryFaceAuthDetail("4512345678901565")).thenReturn(ingDetail);

        EsignFaceAuthCreateResponse esignResp = buildCreateResponse();
        when(esignClient.createFaceAuth(any())).thenReturn(esignResp);
        when(mapper.selectById(100L)).thenReturn(buildResultAuth());

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        RealNameAuthDtos.StartResult result = service.startAuth(TEST_USER_ID, req);

        assertThat(result.authStatus()).isEqualTo(RealNameAuthStatus.VERIFYING.getValue());
        verify(esignClient).createFaceAuth(any());
    }

    // ==================== e签宝状态映射 ====================

    @Test
    void startAuth_shouldMapEsignSuccessToVerifiedAndReject() {
        User user = buildUser();
        when(userService.requireActiveUser(TEST_USER_ID)).thenReturn(user);
        // isVerified: 第一次 false，resolveStaleVerifying 更新 VERIFIED 后第二次 true
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L).thenReturn(1L);

        UserRealNameAuth stale = buildVerifyingAuth();
        stale.setEsignFaceFlowId("4512345678901565");
        stale.setRealName(TEST_REAL_NAME);
        stale.setIdCardType(TEST_ID_CARD_TYPE);

        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stale);

        // e签宝 → SUCCESS（身份匹配）
        when(esignClient.queryFaceAuthDetail("4512345678901565")).thenReturn(buildSuccessDetail());

        RealNameAuthDtos.StartRequest req = new RealNameAuthDtos.StartRequest(
                TEST_REAL_NAME, TEST_ID_CARD_TYPE, TEST_ID_CARD);
        assertThatThrownBy(() -> service.startAuth(TEST_USER_ID, req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409)
                .hasMessageContaining("已完成实名认证");
    }

    // ==================== 脱敏工具方法 ====================

    @Test
    void maskName_shouldHandleVariousLengths() {
        assertThat(RealNameAuthServiceImpl.maskName("张")).isEqualTo("张");
        assertThat(RealNameAuthServiceImpl.maskName("张三")).isEqualTo("张*");
        assertThat(RealNameAuthServiceImpl.maskName("张三丰")).isEqualTo("张**");
        assertThat(RealNameAuthServiceImpl.maskName("测试用户")).isEqualTo("测***");
        assertThat(RealNameAuthServiceImpl.maskName(null)).isNull();
        assertThat(RealNameAuthServiceImpl.maskName("")).isEmpty();
    }

    @Test
    void maskPhone_shouldMaskProperly() {
        assertThat(RealNameAuthServiceImpl.maskPhone("13800138000")).isEqualTo("138****8000");
        assertThat(RealNameAuthServiceImpl.maskPhone(null)).isNull();
        assertThat(RealNameAuthServiceImpl.maskPhone("123")).isEqualTo("123");
    }

    // ==================== 状态处理 ====================

    @Test
    void allKnownStatuses_shouldBeHandled() {
        for (RealNameAuthStatus status : RealNameAuthStatus.values()) {
            RealNameAuthStatus parsed = RealNameAuthStatus.fromValue(status.getValue());
            assertThat(parsed).isEqualTo(status);
        }
    }

    @Test
    void unknownAuthStatus_shouldNotThrow() {
        RealNameAuthStatus parsed = RealNameAuthStatus.fromValue("SOME_UNKNOWN_STATUS");
        assertThat(parsed).isNull();
    }

    @Test
    void unknownAuthStatus_shouldBeSafeInService() {
        // 即使 DB 中存储了未知状态，getStatus 也不应抛异常
        UserRealNameAuth auth = buildVerifyingAuth();
        auth.setAuthStatus("WEIRD_STATUS");
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(auth);

        RealNameAuthDtos.StatusResult result = service.getStatus(TEST_USER_ID);
        assertThat(result.authStatus()).isEqualTo("WEIRD_STATUS");
        // 不抛异常，返回原始状态
    }

    // ==================== helpers ====================

    private User buildUser() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setPhone(TEST_PHONE);
        user.setNickname("测试用户昵称");
        return user;
    }

    private UserRealNameAuth buildVerifyingAuth() {
        UserRealNameAuth auth = new UserRealNameAuth();
        auth.setId(1L);
        auth.setUserId(TEST_USER_ID);
        auth.setRealNameAuthNo(TEST_AUTH_NO);
        auth.setAuthStatus(RealNameAuthStatus.VERIFYING.getValue());
        auth.setRealName(TEST_REAL_NAME);
        auth.setIdCardType(TEST_ID_CARD_TYPE);
        auth.setIdCardCiphertext("v1:mockCiphertext");
        auth.setIdCardMasked("110101********0000");
        auth.setEsignFaceFlowId("4512345678901565");
        auth.setAccountMobile(TEST_PHONE);
        auth.setAuthUrl("https://mock.auth.url");
        auth.setAuthUrlExpireTime(LocalDateTime.now(ZONE).plusHours(1));
        return auth;
    }

    private UserRealNameAuth buildResultAuth() {
        UserRealNameAuth auth = new UserRealNameAuth();
        auth.setId(100L);
        auth.setUserId(TEST_USER_ID);
        auth.setRealNameAuthNo("RNA-NEW-RESULT");
        auth.setAuthStatus(RealNameAuthStatus.VERIFYING.getValue());
        auth.setIdCardMasked("110101********0000");
        auth.setEsignFaceFlowId("4512345678901565");
        auth.setAuthUrl("https://mock.auth.url");
        auth.setAuthUrlExpireTime(LocalDateTime.now(ZONE).plusHours(1));
        return auth;
    }

    private UserRealNameAuth buildVerifiedAuth() {
        UserRealNameAuth auth = new UserRealNameAuth();
        auth.setId(1L);
        auth.setUserId(TEST_USER_ID);
        auth.setRealNameAuthNo(TEST_AUTH_NO);
        auth.setAuthStatus(RealNameAuthStatus.VERIFIED.getValue());
        auth.setIdCardMasked("110101********0000");
        auth.setVerifiedAt(LocalDateTime.now(ZONE));
        return auth;
    }

    private UserRealNameAuth buildExpiredAuth() {
        UserRealNameAuth auth = new UserRealNameAuth();
        auth.setId(1L);
        auth.setUserId(TEST_USER_ID);
        auth.setRealNameAuthNo(TEST_AUTH_NO);
        auth.setAuthStatus(RealNameAuthStatus.EXPIRED.getValue());
        auth.setIdCardMasked("110101********0000");
        auth.setAuthUrlExpireTime(LocalDateTime.now(ZONE).minusHours(1));
        return auth;
    }

    private EsignFaceAuthCreateResponse buildCreateResponse() {
        EsignFaceAuthCreateResponse resp = new EsignFaceAuthCreateResponse();
        resp.setCode(0);
        EsignFaceAuthCreateResponse.CreateFaceAuthData data = new EsignFaceAuthCreateResponse.CreateFaceAuthData();
        data.setFlowId("4512345678901565");
        data.setAuthUrl("https://mock.auth.url");
        data.setExpire(System.currentTimeMillis() + 3600000);
        resp.setData(data);
        return resp;
    }

    private EsignFaceAuthDetailResponse buildSuccessDetail() {
        EsignFaceAuthDetailResponse resp = new EsignFaceAuthDetailResponse();
        resp.setCode(0);
        EsignFaceAuthDetailResponse.FaceAuthDetailData data = new EsignFaceAuthDetailResponse.FaceAuthDetailData();
        data.setFlowId("4512345678901565");
        data.setStatus("SUCCESS");
        data.setObjectType("INDIVIDUAL");
        data.setEndTime(System.currentTimeMillis());
        EsignFaceAuthDetailResponse.FaceAuthDetailData.IndivInfo indivInfo =
                new EsignFaceAuthDetailResponse.FaceAuthDetailData.IndivInfo();
        indivInfo.setName(TEST_REAL_NAME);
        indivInfo.setCertType(TEST_ID_CARD_TYPE);
        indivInfo.setCertNo(TEST_ID_CARD);
        data.setIndivInfo(indivInfo);
        resp.setData(data);
        return resp;
    }
}
