package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.LandlordAuthDtos;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.LandlordAuthApplication;
import com.zhuxiang.service.entity.LandlordAuthProof;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.LandlordAuthApplicationMapper;
import com.zhuxiang.service.mapper.LandlordAuthProofMapper;
import com.zhuxiang.service.mapper.LandlordMapper;
import com.zhuxiang.service.mapper.UserMapper;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.IdCardCryptoService;
import com.zhuxiang.service.service.LandlordAuthService;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LandlordAuthServiceImpl implements LandlordAuthService {
    private static final Set<String> PROOF_TYPES = Set.of(
            "PROPERTY_CERTIFICATE", "PURCHASE_CONTRACT", "LEASE_CERTIFICATE",
            "COURT_DECISION", "OTHER"
    );
    private static final Map<String, String> PROOF_BIZ_TYPES = Map.of(
            "PROPERTY_CERTIFICATE", "landlord_proof_property",
            "PURCHASE_CONTRACT", "landlord_proof_purchase",
            "LEASE_CERTIFICATE", "landlord_proof_lease",
            "COURT_DECISION", "landlord_proof_court",
            "OTHER", "landlord_proof_other"
    );
    private static final Set<String> PLATFORM_ROLES = Set.of("ADMIN", "HOUSEKEEPER");
    private static final DateTimeFormatter APPLICATION_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final LandlordAuthApplicationMapper applicationMapper;
    private final LandlordAuthProofMapper proofMapper;
    private final UserMapper userMapper;
    private final LandlordMapper landlordMapper;
    private final FileRecordService fileRecordService;
    private final IdCardCryptoService idCardCryptoService;
    private final LandlordService landlordService;
    private final MessageService messageService;

    public LandlordAuthServiceImpl(
            LandlordAuthApplicationMapper applicationMapper,
            LandlordAuthProofMapper proofMapper,
            UserMapper userMapper,
            LandlordMapper landlordMapper,
            FileRecordService fileRecordService,
            IdCardCryptoService idCardCryptoService,
            LandlordService landlordService,
            MessageService messageService
    ) {
        this.applicationMapper = applicationMapper;
        this.proofMapper = proofMapper;
        this.userMapper = userMapper;
        this.landlordMapper = landlordMapper;
        this.fileRecordService = fileRecordService;
        this.idCardCryptoService = idCardCryptoService;
        this.landlordService = landlordService;
        this.messageService = messageService;
    }

    @Override
    public LandlordAuthDtos.StatusView getMyStatus(String userId) {
        User user = requireUser(userId);
        LandlordAuthApplication latest = latestForUser(userId);
        boolean landlord = "LANDLORD".equalsIgnoreCase(user.getRole());
        String status = landlord ? "APPROVED" : latest == null ? "UNVERIFIED" : latest.getStatus();
        boolean canSubmit = !landlord && (latest == null
                || Set.of("REJECTED", "SUPERSEDED").contains(latest.getStatus()));
        return new LandlordAuthDtos.StatusView(
                status, canSubmit, landlord, latest == null ? null : toView(latest)
        );
    }

    @Override
    @Transactional
    public LandlordAuthDtos.ApplicationView submit(
            String userId, LandlordAuthDtos.SubmitRequest request
    ) {
        User user = requireUser(userId);
        if ("LANDLORD".equalsIgnoreCase(user.getRole())) {
            throw BusinessException.badRequest("当前账号已经是房东，无需重复认证");
        }
        if (!"TENANT".equalsIgnoreCase(user.getRole())) {
            throw BusinessException.forbidden("当前角色不能申请成为房东");
        }

        LandlordAuthApplication latest = latestForUser(userId);
        if (latest != null && "PENDING".equals(latest.getStatus())) {
            if (!request.replaceExisting()) {
                throw BusinessException.badRequest("已有审核中的房东认证，请先查看认证进度");
            }
            latest.setStatus("SUPERSEDED");
            latest.setUpdatedAt(LocalDateTime.now());
            applicationMapper.updateById(latest);
        }

        fileRecordService.validateFileOwnership(
                userId, request.idCardFrontUrl(), "landlord_id_card_front");
        fileRecordService.validateFileOwnership(
                userId, request.idCardBackUrl(), "landlord_id_card_back");
        validateProofs(userId, request.proofs());

        String normalizedIdCard = request.idCardNo().trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        LandlordAuthApplication application = new LandlordAuthApplication();
        application.setId(UUID.randomUUID().toString());
        application.setApplicationNo("LDA" + now.format(APPLICATION_TIME)
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        application.setUserId(userId);
        application.setStatus("PENDING");
        application.setRealName(request.realName().trim());
        application.setIdCardCiphertext(idCardCryptoService.encrypt(normalizedIdCard));
        application.setIdCardMasked(idCardCryptoService.mask(normalizedIdCard));
        application.setIdCardFrontUrl(request.idCardFrontUrl());
        application.setIdCardBackUrl(request.idCardBackUrl());
        application.setContactPhone(request.contactPhone().trim());
        application.setContactWechat(trimToNull(request.contactWechat()));
        application.setContactEmail(trimToNull(request.contactEmail()));
        application.setContactAddress(trimToNull(request.contactAddress()));
        application.setPreferredContactTime(trimToNull(request.preferredContactTime()));
        application.setApplicantNote(trimToNull(request.applicantNote()));
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        applicationMapper.insert(application);

        for (LandlordAuthDtos.ProofRequest item : request.proofs()) {
            LandlordAuthProof proof = new LandlordAuthProof();
            proof.setId(UUID.randomUUID().toString());
            proof.setApplicationId(application.getId());
            proof.setProofType(item.proofType().trim().toUpperCase());
            proof.setFileId(item.fileId());
            proof.setFileUrl(item.fileUrl());
            proof.setCreatedAt(now);
            proofMapper.insert(proof);
        }
        return toView(application);
    }

    @Override
    public PageData<LandlordAuthDtos.AdminListItem> listAdmin(
            String operatorId, String status, String keyword, long page, long pageSize
    ) {
        requirePlatformStaff(operatorId);
        if (StringUtils.hasText(status)
                && !Set.of("PENDING", "APPROVED", "REJECTED", "SUPERSEDED")
                .contains(status.toUpperCase())) {
            throw BusinessException.badRequest("不支持的认证状态");
        }
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        var query = Wrappers.<LandlordAuthApplication>lambdaQuery()
                .eq(StringUtils.hasText(status), LandlordAuthApplication::getStatus,
                        StringUtils.hasText(status) ? status.toUpperCase() : null)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(LandlordAuthApplication::getApplicationNo, keyword)
                        .or().like(LandlordAuthApplication::getRealName, keyword)
                        .or().like(LandlordAuthApplication::getContactPhone, keyword))
                .orderByAsc(LandlordAuthApplication::getStatus)
                .orderByDesc(LandlordAuthApplication::getCreatedAt);
        Page<LandlordAuthApplication> result = applicationMapper.selectPage(
                new Page<>(safePage, safePageSize), query
        );
        List<LandlordAuthDtos.AdminListItem> items = result.getRecords().stream()
                .map(this::toAdminItem)
                .toList();
        return PageData.of(items, safePage, safePageSize, result.getTotal());
    }

    @Override
    public LandlordAuthDtos.ApplicationView adminDetail(
            String operatorId, String applicationId
    ) {
        requirePlatformStaff(operatorId);
        return toView(requireApplication(applicationId));
    }

    @Override
    @Transactional
    public LandlordAuthDtos.ApplicationView review(
            String operatorId,
            String applicationId,
            LandlordAuthDtos.ReviewRequest request
    ) {
        requirePlatformStaff(operatorId);
        LandlordAuthApplication application = requireApplication(applicationId);
        if (!"PENDING".equals(application.getStatus())) {
            throw BusinessException.badRequest("该申请已处理，不能重复审核");
        }
        String decision = request.decision().toUpperCase();
        String reason = trimToNull(request.reason());
        if ("REJECTED".equals(decision) && !StringUtils.hasText(reason)) {
            throw BusinessException.badRequest("驳回认证时必须填写原因");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = applicationMapper.reviewPending(
                applicationId, decision,
                "REJECTED".equals(decision) ? reason : null,
                operatorId, now
        );
        if (updated != 1) {
            throw BusinessException.badRequest("该申请已被其他审核人员处理");
        }
        application.setStatus(decision);
        application.setRejectReason("REJECTED".equals(decision) ? reason : null);
        application.setReviewerId(operatorId);
        application.setReviewedAt(now);
        application.setUpdatedAt(now);

        if ("APPROVED".equals(decision)) {
            User user = requireUser(application.getUserId());
            if (!"TENANT".equalsIgnoreCase(user.getRole())
                    && !"LANDLORD".equalsIgnoreCase(user.getRole())) {
                throw BusinessException.badRequest("申请人的当前角色不允许升级为房东");
            }
            user.setRole("LANDLORD");
            user.setUpdatedAt(now);
            userMapper.updateById(user);

            Landlord profile = landlordService.ensureProfile(user);
            profile.setName(application.getRealName());
            profile.setPhone(application.getContactPhone());
            profile.setWechat(application.getContactWechat());
            profile.setEmail(application.getContactEmail());
            profile.setContactTime(application.getPreferredContactTime());
            profile.setUpdatedAt(now);
            landlordMapper.updateById(profile);

            messageService.sendMessage(
                    user.getId(), "system", "房东认证已通过",
                    "恭喜您已通过房东身份审核，现在可以进入房东工作台发布和管理房源。",
                    "route", "/landlord/workbench"
            );
        } else {
            messageService.sendMessage(
                    application.getUserId(), "system", "房东认证未通过",
                    "您的房东认证暂未通过：" + reason + "。请修改材料后重新提交。",
                    "route", "/profile/landlord-verify"
            );
        }
        return toView(application);
    }

    private void validateProofs(
            String userId, List<LandlordAuthDtos.ProofRequest> proofs
    ) {
        if (proofs == null || proofs.isEmpty()) {
            throw BusinessException.badRequest("请至少上传一种房源权属证明");
        }
        for (LandlordAuthDtos.ProofRequest proof : proofs) {
            String type = proof.proofType().trim().toUpperCase();
            if (!PROOF_TYPES.contains(type)) {
                throw BusinessException.badRequest("不支持的权属证明类型");
            }
            fileRecordService.validateFileOwnership(
                    userId, proof.fileId(), proof.fileUrl(), PROOF_BIZ_TYPES.get(type)
            );
        }
    }

    private LandlordAuthApplication latestForUser(String userId) {
        return applicationMapper.selectOne(
                Wrappers.<LandlordAuthApplication>lambdaQuery()
                        .eq(LandlordAuthApplication::getUserId, userId)
                        .orderByDesc(LandlordAuthApplication::getCreatedAt)
                        .last("LIMIT 1")
        );
    }

    private LandlordAuthApplication requireApplication(String id) {
        LandlordAuthApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw BusinessException.notFound("房东认证申请不存在");
        }
        return application;
    }

    private User requireUser(String id) {
        User user = userMapper.selectById(id);
        if (user == null || !"active".equalsIgnoreCase(user.getStatus())) {
            throw BusinessException.forbidden("用户不存在或不可用");
        }
        return user;
    }

    private User requirePlatformStaff(String operatorId) {
        User operator = requireUser(operatorId);
        if (!PLATFORM_ROLES.contains(operator.getRole())) {
            throw BusinessException.forbidden("无权审核房东认证");
        }
        return operator;
    }

    private LandlordAuthDtos.ApplicationView toView(LandlordAuthApplication value) {
        List<LandlordAuthDtos.ProofView> proofs = proofMapper.selectList(
                Wrappers.<LandlordAuthProof>lambdaQuery()
                        .eq(LandlordAuthProof::getApplicationId, value.getId())
                        .orderByAsc(LandlordAuthProof::getCreatedAt)
        ).stream().map(item -> new LandlordAuthDtos.ProofView(
                item.getId(), item.getProofType(), item.getFileId(), item.getFileUrl()
        )).toList();
        return new LandlordAuthDtos.ApplicationView(
                value.getId(), value.getApplicationNo(), value.getUserId(), value.getStatus(),
                value.getRealName(), value.getIdCardMasked(), value.getIdCardFrontUrl(),
                value.getIdCardBackUrl(), value.getContactPhone(), value.getContactWechat(),
                value.getContactEmail(), value.getContactAddress(), value.getPreferredContactTime(),
                value.getApplicantNote(), value.getRejectReason(), value.getReviewerId(),
                value.getReviewedAt(), value.getCreatedAt(), value.getUpdatedAt(), proofs
        );
    }

    private LandlordAuthDtos.AdminListItem toAdminItem(LandlordAuthApplication value) {
        User user = userMapper.selectById(value.getUserId());
        int proofCount = Math.toIntExact(proofMapper.selectCount(
                Wrappers.<LandlordAuthProof>lambdaQuery()
                        .eq(LandlordAuthProof::getApplicationId, value.getId())
        ));
        return new LandlordAuthDtos.AdminListItem(
                value.getId(), value.getApplicationNo(), value.getUserId(), value.getRealName(),
                value.getContactPhone(), user == null ? "" : user.getNickname(),
                user == null ? "" : user.getPhone(), value.getStatus(), proofCount,
                value.getCreatedAt(), value.getReviewedAt()
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
