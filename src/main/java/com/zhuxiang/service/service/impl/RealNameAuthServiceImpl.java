package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zhuxiang.service.client.EsignFaceAuthClient;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.EsignApiException;
import com.zhuxiang.service.config.EsignFaceProperties;
import com.zhuxiang.service.dto.EsignFaceAuthCreateRequest;
import com.zhuxiang.service.dto.EsignFaceAuthCreateResponse;
import com.zhuxiang.service.dto.EsignFaceAuthDetailResponse;
import com.zhuxiang.service.dto.RealNameAuthDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.entity.UserRealNameAuth;
import com.zhuxiang.service.enums.RealNameAuthStatus;
import com.zhuxiang.service.mapper.UserRealNameAuthMapper;
import com.zhuxiang.service.service.IdCardCryptoService;
import com.zhuxiang.service.service.RealNameAuthService;
import com.zhuxiang.service.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 个人实名认证业务服务实现。
 * <p>
 * 事务边界：短事务操作 DB，远程 HTTP 调用在事务外执行。
 * 并发控制：同一 user_id 在同一时刻只允许存在一个 VERIFYING 记录，
 * 通过 DB 唯一约束 + SELECT FOR UPDATE 保证。
 */
@Service
public class RealNameAuthServiceImpl implements RealNameAuthService {

    private static final Logger log = LoggerFactory.getLogger(RealNameAuthServiceImpl.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** e签宝状态 → 本地终态（VERIFYING 本身不在此集合中，因为 ING 映射为 VERIFYING 后还需继续判断） */
    private static final Set<String> E_SIGN_SUCCESS_STATUSES = Set.of("SUCCESS", "COMPLETED");
    private static final Set<String> E_SIGN_FAILED_STATUSES = Set.of("FAILED", "FAIL", "REJECTED");
    private static final Set<String> E_SIGN_CANCELED_STATUSES = Set.of("CANCELED", "CANCELLED");
    private static final Set<String> E_SIGN_EXPIRED_STATUSES = Set.of("EXPIRED", "TIMEOUT");
    private static final Set<String> E_SIGN_ING_STATUSES = Set.of("ING", "PROCESSING");

    private final UserRealNameAuthMapper mapper;
    private final UserService userService;
    private final IdCardCryptoService cryptoService;
    private final EsignFaceAuthClient esignClient;
    private final EsignFaceProperties esignProperties;
    private RealNameAuthServiceImpl self;

    public RealNameAuthServiceImpl(UserRealNameAuthMapper mapper,
                                   UserService userService,
                                   IdCardCryptoService cryptoService,
                                   EsignFaceAuthClient esignClient,
                                   EsignFaceProperties esignProperties) {
        this.mapper = mapper;
        this.userService = userService;
        this.cryptoService = cryptoService;
        this.esignClient = esignClient;
        this.esignProperties = esignProperties;
    }

    /** 注入自身代理，使 @Transactional 方法通过代理调用生效 */
    @Lazy
    @jakarta.annotation.Resource
    public void setSelf(RealNameAuthServiceImpl self) {
        this.self = self;
    }

    // ==================== 发起认证 ====================

    @Override
    public RealNameAuthDtos.StartResult startAuth(String userId, RealNameAuthDtos.StartRequest request) {
        User user = userService.requireActiveUser(userId);
        String realName = request.realName().trim();
        String idCardNo = request.idCardNo().trim();
        String idCardType = request.idCardType();

        // 检查是否已认证
        if (isVerified(userId)) {
            throw BusinessException.conflict("您已完成实名认证，无需重新认证");
        }

        // 如果已有 VERIFYING 记录且有 flowId → 先查询 e签宝确认真实状态
        resolveStaleVerifying(userId);

        // resolveStaleVerifying 可能已将状态更新为 VERIFIED，需要重新检查
        if (isVerified(userId)) {
            throw BusinessException.conflict("您已完成实名认证，无需重新认证");
        }

        // 事务中：检查现有 VERIFYING + 创建新记录（防止并发）
        CreateResult createResult = self.checkAndCreateVerifying(userId, realName, idCardType, idCardNo, user.getPhone(), null);

        if (createResult.existingValid) {
            throw BusinessException.conflict("您已有进行中的认证任务，请先完成或等待过期后再试");
        }

        return callEsignAndUpdate(userId, realName, idCardType, idCardNo, createResult.auth);
    }

    // ==================== 重新发起认证 ====================

    @Override
    public RealNameAuthDtos.StartResult restartAuth(String userId, RealNameAuthDtos.RestartRequest request) {
        User user = userService.requireActiveUser(userId);
        String realName = request.realName().trim();
        String idCardNo = request.idCardNo().trim();
        String idCardType = request.idCardType();

        if (isVerified(userId)) {
            throw BusinessException.conflict("您已完成实名认证，无需重新认证");
        }

        // 事务中：将旧 VERIFYING 强制过期 + 创建新记录
        CreateResult createResult = self.forceExpireAndCreate(userId, realName, idCardType, idCardNo, user.getPhone());

        if (createResult.existingValid) {
            throw BusinessException.conflict("您已有进行中的认证任务，请先完成或等待过期后再试");
        }

        return callEsignAndUpdate(userId, realName, idCardType, idCardNo, createResult.auth);
    }

    // ==================== e签宝调用 + 结果保存（start/restart 共用） ====================

    private RealNameAuthDtos.StartResult callEsignAndUpdate(String userId, String realName,
                                                             String idCardType, String idCardNo,
                                                             UserRealNameAuth auth) {
        try {
            EsignFaceAuthCreateRequest esignReq = buildEsignRequest(realName, idCardType, idCardNo, auth.getRealNameAuthNo());
            EsignFaceAuthCreateResponse esignResp = esignClient.createFaceAuth(esignReq);

            self.updateWithEsignResult(auth.getId(), esignResp);

            UserRealNameAuth updated = mapper.selectById(auth.getId());
            log.info("发起实名认证成功: userId={}, realNameAuthNo={}, flowId={}",
                    userId, auth.getRealNameAuthNo(), EsignFaceAuthClient.maskFlowId(updated.getEsignFaceFlowId()));
            return toStartResult(updated);

        } catch (EsignApiException e) {
            self.updateStatusIfVerifying(auth.getId(), RealNameAuthStatus.FAILED,
                    e.getEsignCode() != 0 ? String.valueOf(e.getEsignCode()) : null, null);
            log.warn("e签宝创建认证任务失败: userId={}, realNameAuthNo={}, esignCode={}",
                    userId, auth.getRealNameAuthNo(), e.getEsignCode());
            throw e;
        } catch (Exception e) {
            self.updateStatusIfVerifying(auth.getId(), RealNameAuthStatus.FAILED, null, null);
            log.error("发起实名认证网络异常: userId={}, realNameAuthNo={}", userId, auth.getRealNameAuthNo(), e);
            throw new EsignApiException(0, 0, "e签宝认证任务创建失败", "/v2/identity/auth/api/individual/face");
        }
    }

    // ==================== 处理僵死 VERIFYING（start 调用） ====================

    /**
     * 如果用户已有 VERIFYING 记录且附带 flowId，先查询 e签宝确认真实状态。
     * <p>
     * - e签宝终态（VERIFIED/FAILED/CANCELED/EXPIRED）→ 更新本地状态，允许后续创建新任务。
     * - e签宝 ING + 认证链接有效 → 由后续 checkAndCreateVerifying 返回 409。
     * - e签宝 ING + 认证链接过期 → 更新本地为 EXPIRED，允许创建新任务。
     */
    private void resolveStaleVerifying(String userId) {
        UserRealNameAuth existing = findLatestVerifying(userId);
        if (existing == null || existing.getEsignFaceFlowId() == null || existing.getEsignFaceFlowId().isBlank()) {
            return;
        }

        EsignFaceAuthDetailResponse detail;
        try {
            detail = esignClient.queryFaceAuthDetail(existing.getEsignFaceFlowId());
        } catch (Exception e) {
            log.warn("start 前查询 e签宝状态失败: userId={}, realNameAuthNo={}, flowId={}",
                    userId, existing.getRealNameAuthNo(),
                    EsignFaceAuthClient.maskFlowId(existing.getEsignFaceFlowId()));
            return; // 查询失败不阻塞，交给后续 checkAndCreateVerifying 判断
        }

        if (detail.getData() == null) {
            return;
        }

        RealNameAuthStatus mapped = mapEsignStatus(detail.getData().getStatus());
        log.info("e签宝状态映射: userId={}, realNameAuthNo={}, providerStatus={} -> {}",
                userId, existing.getRealNameAuthNo(), detail.getData().getStatus(), mapped.getValue());

        if (mapped == RealNameAuthStatus.VERIFIED) {
            // 校验身份信息后更新
            if (verifyDetailMatches(existing, detail.getData())) {
                self.updateToVerified(existing.getId(), detail.getData(),
                        detail.getData().getIndivInfo() != null ? detail.getData().getIndivInfo().getMobileNo() : null);
            }
        } else if (mapped != RealNameAuthStatus.VERIFYING) {
            // FAILED / CANCELED / EXPIRED → 直接更新为终态
            self.updateStatusIfVerifying(existing.getId(), mapped, null, null);
        }
        // VERIFYING 保持不变，后续 checkAndCreateVerifying 判断是否过期
    }

    // ==================== 事务方法 ====================

    /**
     * 事务内：SELECT FOR UPDATE 锁住用户现有 VERIFYING 记录，若过期则清理，然后创建新记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateResult checkAndCreateVerifying(String userId, String realName,
                                                  String idCardType, String idCardNo,
                                                  String phone, String expireReason) {
        UserRealNameAuth existing = lockVerifying(userId);
        if (existing != null) {
            if (isAuthUrlValid(existing)) {
                return CreateResult.existingValid();
            }
            expireVerifying(existing.getId(), expireReason);
        }
        return CreateResult.created(insertVerifying(userId, realName, idCardType, idCardNo, phone));
    }

    /**
     * 事务内：强制将旧 VERIFYING 过期（无论 authUrl 是否有效），然后创建新记录。
     * <p>
     * 仅当旧 VERIFYING 有有效 authUrl 且被并发请求抢先创建了新 VERIFYING 时才返回 existingValid。
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateResult forceExpireAndCreate(String userId, String realName,
                                               String idCardType, String idCardNo, String phone) {
        UserRealNameAuth existing = lockVerifying(userId);
        if (existing != null) {
            expireVerifying(existing.getId(), "USER_RESTARTED");
            log.info("用户 {} 重新发起认证，旧 VERIFYING 记录 {} 已过期", userId, existing.getRealNameAuthNo());
        }
        // 过期后再次检查（并发场景：另一个线程可能已插入新 VERIFYING）
        UserRealNameAuth afterExpire = lockVerifying(userId);
        if (afterExpire != null && isAuthUrlValid(afterExpire)) {
            return CreateResult.existingValid();
        }
        return CreateResult.created(insertVerifying(userId, realName, idCardType, idCardNo, phone));
    }

    /** SELECT FOR UPDATE 锁住用户当前 VERIFYING 行 */
    private UserRealNameAuth lockVerifying(String userId) {
        return mapper.selectOne(new LambdaQueryWrapper<UserRealNameAuth>()
                .eq(UserRealNameAuth::getUserId, userId)
                .eq(UserRealNameAuth::getAuthStatus, RealNameAuthStatus.VERIFYING.getValue())
                .orderByDesc(UserRealNameAuth::getCreatedAt)
                .last("LIMIT 1 FOR UPDATE"));
    }

    /** 原子更新 VERIFYING → EXPIRED */
    private void expireVerifying(Long id, String expireReason) {
        UpdateWrapper<UserRealNameAuth> wrapper = new UpdateWrapper<UserRealNameAuth>()
                .eq("id", id)
                .eq("auth_status", RealNameAuthStatus.VERIFYING.getValue())
                .set("auth_status", RealNameAuthStatus.EXPIRED.getValue())
                .set("updated_at", LocalDateTime.now(ZONE));
        if (expireReason != null) {
            wrapper.set("expire_reason", expireReason);
        }
        mapper.update(null, wrapper);
    }

    /** 插入新的 VERIFYING 记录 */
    private UserRealNameAuth insertVerifying(String userId, String realName,
                                               String idCardType, String idCardNo, String phone) {
        UserRealNameAuth auth = new UserRealNameAuth();
        auth.setUserId(userId);
        auth.setRealNameAuthNo(generateAuthNo());
        auth.setAuthStatus(RealNameAuthStatus.VERIFYING.getValue());
        auth.setRealName(realName);
        auth.setAccountMobile(phone);
        auth.setIdCardType(idCardType);
        auth.setIdCardCiphertext(cryptoService.encrypt(idCardNo));
        auth.setIdCardMasked(cryptoService.mask(idCardNo));
        auth.setCreatedAt(LocalDateTime.now(ZONE));
        auth.setUpdatedAt(LocalDateTime.now(ZONE));
        auth.setVersion(0);
        mapper.insert(auth);
        return auth;
    }

    /** checkAndCreateVerifying / forceExpireAndCreate 返回值 */
    public static class CreateResult {
        final boolean existingValid;
        final UserRealNameAuth auth;

        private CreateResult(boolean existingValid, UserRealNameAuth auth) {
            this.existingValid = existingValid;
            this.auth = auth;
        }

        static CreateResult existingValid() { return new CreateResult(true, null); }
        static CreateResult created(UserRealNameAuth auth) { return new CreateResult(false, auth); }
    }

    // ==================== e签宝状态映射 ====================

    /**
     * 将 e签宝 provider status 映射为本地 {@link RealNameAuthStatus}。
     * <p>
     * 未知状态返回 VERIFYING（安全默认值，不丢失已有 authUrl）。
     */
    private RealNameAuthStatus mapEsignStatus(String providerStatus) {
        if (providerStatus == null || providerStatus.isBlank()) {
            return RealNameAuthStatus.VERIFYING;
        }
        String upper = providerStatus.toUpperCase(Locale.ROOT);
        if (E_SIGN_SUCCESS_STATUSES.contains(upper)) {
            return RealNameAuthStatus.VERIFIED;
        }
        if (E_SIGN_FAILED_STATUSES.contains(upper)) {
            return RealNameAuthStatus.FAILED;
        }
        if (E_SIGN_CANCELED_STATUSES.contains(upper)) {
            return RealNameAuthStatus.CANCELED;
        }
        if (E_SIGN_EXPIRED_STATUSES.contains(upper)) {
            return RealNameAuthStatus.EXPIRED;
        }
        if (E_SIGN_ING_STATUSES.contains(upper)) {
            return RealNameAuthStatus.VERIFYING;
        }
        return RealNameAuthStatus.VERIFYING; // 未知 → 安全默认
    }

    // ==================== 刷新认证 ====================

    @Override
    public RealNameAuthDtos.RefreshResult refreshAuth(String userId, String realNameAuthNo) {
        UserRealNameAuth auth = findByRealNameAuthNo(realNameAuthNo);
        if (auth == null) {
            throw BusinessException.notFound("认证记录不存在");
        }
        if (!auth.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权操作该认证记录");
        }

        if (RealNameAuthStatus.VERIFIED.getValue().equals(auth.getAuthStatus())) {
            return toRefreshResult(auth);
        }

        if (RealNameAuthStatus.VERIFYING.getValue().equals(auth.getAuthStatus())
                && auth.getAuthUrlExpireTime() != null
                && !isAuthUrlValid(auth)) {
            self.updateStatusIfVerifying(auth.getId(), RealNameAuthStatus.EXPIRED, null, null);
            auth = mapper.selectById(auth.getId());
            return toRefreshResult(auth);
        }

        if (auth.getEsignFaceFlowId() == null || auth.getEsignFaceFlowId().isBlank()) {
            return toRefreshResult(auth);
        }

        EsignFaceAuthDetailResponse detail;
        try {
            detail = esignClient.queryFaceAuthDetail(auth.getEsignFaceFlowId());
        } catch (EsignApiException e) {
            log.warn("e签宝状态查询失败: userId={}, realNameAuthNo={}, esignCode={}",
                    userId, realNameAuthNo, e.getEsignCode());
            return toRefreshResult(auth);
        } catch (Exception e) {
            log.warn("e签宝状态查询暂时失败: userId={}, realNameAuthNo={}", userId, realNameAuthNo);
            return toRefreshResult(auth);
        }

        processQueryResult(auth, detail);

        UserRealNameAuth updated = mapper.selectById(auth.getId());
        return toRefreshResult(updated);
    }

    // ==================== 查询状态 ====================

    @Override
    public RealNameAuthDtos.StatusResult getStatus(String userId) {
        UserRealNameAuth latest = findLatest(userId);
        if (latest == null) {
            return new RealNameAuthDtos.StatusResult(
                    null,
                    RealNameAuthStatus.UNVERIFIED.getValue(),
                    null, null, null, null, null, null, null
            );
        }

        if (RealNameAuthStatus.VERIFYING.getValue().equals(latest.getAuthStatus())
                && latest.getAuthUrlExpireTime() != null
                && !isAuthUrlValid(latest)) {
            self.updateStatusIfVerifying(latest.getId(), RealNameAuthStatus.EXPIRED, null, null);
            latest = mapper.selectById(latest.getId());
        }

        return buildStatusResult(latest);
    }

    @Override
    public UserRealNameAuth getVerifiedRecord(String userId) {
        return mapper.selectOne(new LambdaQueryWrapper<UserRealNameAuth>()
                .eq(UserRealNameAuth::getUserId, userId)
                .eq(UserRealNameAuth::getAuthStatus, RealNameAuthStatus.VERIFIED.getValue())
                .orderByDesc(UserRealNameAuth::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    public boolean isVerified(String userId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<UserRealNameAuth>()
                .eq(UserRealNameAuth::getUserId, userId)
                .eq(UserRealNameAuth::getAuthStatus, RealNameAuthStatus.VERIFIED.getValue()));
        return count != null && count > 0;
    }

    @Override
    public void requireVerified(String userId) {
        if (!isVerified(userId)) {
            throw BusinessException.forbidden("请先完成实名认证");
        }
    }

    // ==================== 事务方法（供 self 代理调用） ====================

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void updateWithEsignResult(Long id, EsignFaceAuthCreateResponse response) {
        EsignFaceAuthCreateResponse.CreateFaceAuthData data = response.getData();
        UpdateWrapper<UserRealNameAuth> wrapper = new UpdateWrapper<UserRealNameAuth>()
                .eq("id", id)
                .eq("auth_status", RealNameAuthStatus.VERIFYING.getValue())
                .set("esign_face_flow_id", data.getFlowId())
                .set("auth_url", data.getAuthUrl())
                .set("auth_url_expire_time", millisToDateTime(data.getExpire()))
                .set("updated_at", LocalDateTime.now(ZONE));
        mapper.update(null, wrapper);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void updateStatusIfVerifying(Long id, RealNameAuthStatus status, String failureCode, String expireReason) {
        UpdateWrapper<UserRealNameAuth> wrapper = new UpdateWrapper<UserRealNameAuth>()
                .eq("id", id)
                .eq("auth_status", RealNameAuthStatus.VERIFYING.getValue())
                .set("auth_status", status.getValue())
                .set("updated_at", LocalDateTime.now(ZONE));
        if (failureCode != null) {
            wrapper.set("failure_code", failureCode);
        }
        if (expireReason != null) {
            wrapper.set("expire_reason", expireReason);
        }
        mapper.update(null, wrapper);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void updateToVerified(Long id, EsignFaceAuthDetailResponse.FaceAuthDetailData data, String verifiedMobile) {
        UpdateWrapper<UserRealNameAuth> wrapper = new UpdateWrapper<UserRealNameAuth>()
                .eq("id", id)
                .eq("auth_status", RealNameAuthStatus.VERIFYING.getValue())
                .set("auth_status", RealNameAuthStatus.VERIFIED.getValue())
                .set("verified_at", millisToDateTime(data.getEndTime()))
                .set("verified_mobile", verifiedMobile)
                .set("auth_url", null)
                .set("updated_at", LocalDateTime.now(ZONE));
        mapper.update(null, wrapper);
    }

    // ==================== 私有方法 ====================

    private void processQueryResult(UserRealNameAuth auth, EsignFaceAuthDetailResponse detail) {
        EsignFaceAuthDetailResponse.FaceAuthDetailData data = detail.getData();
        if (data == null) {
            log.warn("e签宝查询详情 data 为空: flowId={}", EsignFaceAuthClient.maskFlowId(auth.getEsignFaceFlowId()));
            return;
        }

        if (!auth.getEsignFaceFlowId().equals(data.getFlowId())) {
            log.warn("flowId 不一致: local={}, remote={}",
                    EsignFaceAuthClient.maskFlowId(auth.getEsignFaceFlowId()),
                    EsignFaceAuthClient.maskFlowId(data.getFlowId()));
            return;
        }

        RealNameAuthStatus mapped = mapEsignStatus(data.getStatus());

        if (mapped == RealNameAuthStatus.VERIFIED) {
            if (verifyDetailMatches(auth, data)) {
                String verifiedMobile = data.getIndivInfo() != null ? data.getIndivInfo().getMobileNo() : null;
                self.updateToVerified(auth.getId(), data, verifiedMobile);
                log.info("实名认证成功: userId={}, realNameAuthNo={}, flowId={}",
                        auth.getUserId(), auth.getRealNameAuthNo(),
                        EsignFaceAuthClient.maskFlowId(auth.getEsignFaceFlowId()));
            } else {
                log.warn("认证成功但身份信息不一致: userId={}, realNameAuthNo={}, objectType={}",
                        auth.getUserId(), auth.getRealNameAuthNo(), data.getObjectType());
                throw new EsignApiException(200, 0,
                        "e签宝返回身份信息与本地不一致",
                        "/v2/identity/auth/api/common/" + auth.getEsignFaceFlowId() + "/detail");
            }
        } else if (mapped != RealNameAuthStatus.VERIFYING) {
            // FAILED / CANCELED / EXPIRED
            self.updateStatusIfVerifying(auth.getId(), mapped, null, null);
            log.info("认证终态: userId={}, realNameAuthNo={}, status={}, providerStatus={}",
                    auth.getUserId(), auth.getRealNameAuthNo(), mapped.getValue(), data.getStatus());
        } else {
            log.info("认证处理中: userId={}, realNameAuthNo={}, providerStatus={}",
                    auth.getUserId(), auth.getRealNameAuthNo(), data.getStatus());
        }
    }

    private boolean verifyDetailMatches(UserRealNameAuth auth, EsignFaceAuthDetailResponse.FaceAuthDetailData data) {
        return "INDIVIDUAL".equals(data.getObjectType())
                && data.getIndivInfo() != null
                && auth.getRealName().equals(data.getIndivInfo().getName())
                && auth.getIdCardType().equals(data.getIndivInfo().getCertType())
                && verifyCertNo(auth, data.getIndivInfo().getCertNo());
    }

    private boolean verifyCertNo(UserRealNameAuth auth, String certNo) {
        if (certNo == null || certNo.isBlank()) {
            return false;
        }
        try {
            String decrypted = cryptoService.decrypt(auth.getIdCardCiphertext());
            return decrypted.equals(certNo);
        } catch (Exception e) {
            log.warn("身份证解密或比对失败: realNameAuthNo={}", auth.getRealNameAuthNo());
            return false;
        }
    }

    private UserRealNameAuth findLatestVerifying(String userId) {
        return mapper.selectOne(new LambdaQueryWrapper<UserRealNameAuth>()
                .eq(UserRealNameAuth::getUserId, userId)
                .eq(UserRealNameAuth::getAuthStatus, RealNameAuthStatus.VERIFYING.getValue())
                .orderByDesc(UserRealNameAuth::getCreatedAt)
                .last("LIMIT 1"));
    }

    private UserRealNameAuth findLatest(String userId) {
        return mapper.selectOne(new LambdaQueryWrapper<UserRealNameAuth>()
                .eq(UserRealNameAuth::getUserId, userId)
                .orderByDesc(UserRealNameAuth::getCreatedAt)
                .last("LIMIT 1"));
    }

    private UserRealNameAuth findByRealNameAuthNo(String realNameAuthNo) {
        return mapper.selectOne(new LambdaQueryWrapper<UserRealNameAuth>()
                .eq(UserRealNameAuth::getRealNameAuthNo, realNameAuthNo));
    }

    private boolean isAuthUrlValid(UserRealNameAuth auth) {
        if (auth.getAuthUrlExpireTime() == null || auth.getAuthUrl() == null) {
            return false;
        }
        return LocalDateTime.now(ZONE).isBefore(auth.getAuthUrlExpireTime());
    }

    private EsignFaceAuthCreateRequest buildEsignRequest(String realName, String idCardType, String idCardNo, String contextId) {
        EsignFaceAuthCreateRequest req = new EsignFaceAuthCreateRequest();
        req.setName(realName);
        req.setCertType(idCardType);
        req.setIdNo(idCardNo);
        req.setFaceauthMode(esignProperties.getMode());
        req.setFaceInterfaceType("H5");
        req.setResultPage("1");
        req.setCallbackUrl(esignProperties.getCallbackUrl());
        req.setContextId(contextId);
        return req;
    }

    private LocalDateTime millisToDateTime(Long millis) {
        if (millis == null) {
            return null;
        }
        return Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDateTime();
    }

    // ==================== DTO 构造 ====================

    private RealNameAuthDtos.StartResult toStartResult(UserRealNameAuth auth) {
        String expireTime = auth.getAuthUrlExpireTime() != null
                ? auth.getAuthUrlExpireTime().toString() : null;
        return new RealNameAuthDtos.StartResult(
                auth.getRealNameAuthNo(), auth.getAuthStatus(),
                auth.getIdCardMasked(), auth.getAuthUrl(), expireTime);
    }

    private RealNameAuthDtos.RefreshResult toRefreshResult(UserRealNameAuth auth) {
        String verifiedAt = auth.getVerifiedAt() != null ? auth.getVerifiedAt().toString() : null;
        String authUrl = null;
        String authUrlExpireTime = null;
        if (RealNameAuthStatus.VERIFYING.getValue().equals(auth.getAuthStatus())
                && auth.getAuthUrlExpireTime() != null
                && isAuthUrlValid(auth)) {
            authUrl = auth.getAuthUrl();
            authUrlExpireTime = auth.getAuthUrlExpireTime().toString();
        }
        return new RealNameAuthDtos.RefreshResult(
                auth.getRealNameAuthNo(),
                auth.getAuthStatus(),
                maskName(auth.getRealName()),
                auth.getIdCardMasked(),
                maskPhone(auth.getAccountMobile()),
                maskPhone(auth.getVerifiedMobile()),
                verifiedAt,
                authUrl,
                authUrlExpireTime);
    }

    private RealNameAuthDtos.StatusResult buildStatusResult(UserRealNameAuth record) {
        String authUrlExpireTime = null;
        String authUrl = null;
        if (RealNameAuthStatus.VERIFYING.getValue().equals(record.getAuthStatus())
                && record.getAuthUrlExpireTime() != null
                && isAuthUrlValid(record)) {
            authUrlExpireTime = record.getAuthUrlExpireTime().toString();
            authUrl = record.getAuthUrl();
        }

        String verifiedAt = record.getVerifiedAt() != null
                ? record.getVerifiedAt().toString() : null;

        return new RealNameAuthDtos.StatusResult(
                record.getRealNameAuthNo(),
                record.getAuthStatus(),
                maskName(record.getRealName()),
                record.getIdCardMasked(),
                maskPhone(record.getAccountMobile()),
                maskPhone(record.getVerifiedMobile()),
                verifiedAt,
                authUrlExpireTime,
                authUrl
        );
    }

    // ==================== 脱敏工具方法 ====================

    private static String generateAuthNo() {
        return "RNA" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name.charAt(0));
        sb.append("*".repeat(name.length() - 1));
        return sb.toString();
    }
}
