package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.LandlordDtos;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.LandlordMapper;
import com.zhuxiang.service.mapper.UserMapper;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.service.RealNameVerificationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class LandlordServiceImpl extends ServiceImpl<LandlordMapper, Landlord>
        implements LandlordService {

    private final UserMapper userMapper;
    private final RealNameVerificationQueryService realNameVerificationQueryService;

    public LandlordServiceImpl(
            UserMapper userMapper,
            RealNameVerificationQueryService realNameVerificationQueryService
    ) {
        this.userMapper = userMapper;
        this.realNameVerificationQueryService = realNameVerificationQueryService;
    }

    @Override
    public Landlord findByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return getOne(
                Wrappers.<Landlord>lambdaQuery()
                        .eq(Landlord::getUserId, userId)
                        .last("LIMIT 1"),
                false
        );
    }

    @Override
    public Landlord requireByUserId(String userId) {
        Landlord landlord = findByUserId(userId);
        if (landlord == null) {
            throw BusinessException.notFound("房东资料不存在");
        }
        return landlord;
    }

    @Override
    @Transactional
    public Landlord ensureProfile(User user) {
        requireLandlordUser(user);
        Landlord existing = findByUserId(user.getId());
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        Landlord profile = new Landlord();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(user.getId());
        profile.setName(defaultName(user));
        profile.setAvatarUrl(valueOrEmpty(user.getAvatarUrl()));
        profile.setPhone(user.getPhone());
        profile.setShowPhone(0);
        profile.setShowWechat(0);
        profile.setShowEmail(0);
        profile.setServiceYears(0);
        profile.setIsVerified(realNameVerificationQueryService.isVerified(user.getId()) ? 1 : 0);
        profile.setRating(BigDecimal.ZERO);
        profile.setRentedCount(0);
        profile.setResponseDescription("通常会及时回复");
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        save(profile);
        return profile;
    }

    @Override
    public LandlordDtos.ProfileView getPublicProfile(String userId) {
        User user = requireLandlordUser(userMapper.selectById(userId));
        return toView(requireByUserId(userId), user, false);
    }

    @Override
    @Transactional
    public LandlordDtos.ProfileView getMyProfile(String userId) {
        User user = requireLandlordUser(userMapper.selectById(userId));
        return toView(ensureProfile(user), user, true);
    }

    @Override
    @Transactional
    public LandlordDtos.ProfileView updateMyProfile(
            String userId,
            LandlordDtos.UpdateLandlordProfileRequest request
    ) {
        User user = requireLandlordUser(userMapper.selectById(userId));
        Landlord profile = ensureProfile(user);

        if (request.name() != null) {
            String name = request.name().trim();
            if (!StringUtils.hasText(name)) {
                throw BusinessException.badRequest("展示名称不能为空");
            }
            profile.setName(name);
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(normalizeOptional(request.avatarUrl()));
        }
        if (request.coverImageUrl() != null) {
            profile.setCoverImageUrl(normalizeOptional(request.coverImageUrl()));
        }
        if (request.slogan() != null) {
            profile.setSlogan(normalizeOptional(request.slogan()));
        }
        if (request.introduction() != null) {
            profile.setIntroduction(normalizeOptional(request.introduction()));
        }
        if (request.serviceArea() != null) {
            profile.setServiceArea(normalizeOptional(request.serviceArea()));
        }
        if (request.serviceYears() != null) {
            profile.setServiceYears(request.serviceYears());
        }
        if (request.profileTags() != null) {
            profile.setProfileTags(encodeTags(request.profileTags()));
        }
        if (request.phone() != null) {
            profile.setPhone(normalizeOptional(request.phone()));
        }
        if (request.wechat() != null) {
            profile.setWechat(normalizeOptional(request.wechat()));
        }
        if (request.email() != null) {
            profile.setEmail(normalizeOptional(request.email()));
        }
        if (request.contactTime() != null) {
            profile.setContactTime(normalizeOptional(request.contactTime()));
        }
        if (request.responseDescription() != null) {
            profile.setResponseDescription(normalizeOptional(request.responseDescription()));
        }
        if (request.showPhone() != null) {
            profile.setShowPhone(request.showPhone() ? 1 : 0);
        }
        if (request.showWechat() != null) {
            profile.setShowWechat(request.showWechat() ? 1 : 0);
        }
        if (request.showEmail() != null) {
            profile.setShowEmail(request.showEmail() ? 1 : 0);
        }
        validateVisibleContacts(profile);

        profile.setUpdatedAt(LocalDateTime.now());
        updateById(profile);
        return toView(profile, user, true);
    }

    private LandlordDtos.ProfileView toView(Landlord profile, User user, boolean ownerView) {
        boolean showPhone = integerBoolean(profile.getShowPhone());
        boolean showWechat = integerBoolean(profile.getShowWechat());
        boolean showEmail = integerBoolean(profile.getShowEmail());
        return new LandlordDtos.ProfileView(
                profile.getUserId(),
                valueOrEmpty(profile.getName()),
                valueOrEmpty(profile.getAvatarUrl()),
                valueOrEmpty(profile.getCoverImageUrl()),
                valueOrEmpty(profile.getSlogan()),
                valueOrEmpty(profile.getIntroduction()),
                realNameVerificationQueryService.isVerified(user.getId()),
                profile.getRating() == null ? BigDecimal.ZERO : profile.getRating(),
                profile.getRentedCount() == null ? 0 : profile.getRentedCount(),
                valueOrEmpty(profile.getResponseDescription()),
                valueOrEmpty(profile.getServiceArea()),
                profile.getServiceYears() == null ? 0 : profile.getServiceYears(),
                decodeTags(profile.getProfileTags()),
                ownerView || showPhone ? profile.getPhone() : null,
                ownerView || showWechat ? profile.getWechat() : null,
                ownerView || showEmail ? profile.getEmail() : null,
                valueOrEmpty(profile.getContactTime()),
                showPhone,
                showWechat,
                showEmail,
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private User requireLandlordUser(User user) {
        if (user == null) {
            throw BusinessException.notFound("房东用户不存在");
        }
        if (!"LANDLORD".equals(user.getRole())) {
            throw BusinessException.forbidden("仅房东账号可以维护房东资料");
        }
        return user;
    }

    private void validateVisibleContacts(Landlord profile) {
        if (integerBoolean(profile.getShowPhone()) && !StringUtils.hasText(profile.getPhone())) {
            throw BusinessException.badRequest("公开手机号前请先填写联系电话");
        }
        if (integerBoolean(profile.getShowWechat()) && !StringUtils.hasText(profile.getWechat())) {
            throw BusinessException.badRequest("公开微信前请先填写微信号");
        }
        if (integerBoolean(profile.getShowEmail()) && !StringUtils.hasText(profile.getEmail())) {
            throw BusinessException.badRequest("公开邮箱前请先填写联系邮箱");
        }
    }

    private String encodeTags(List<String> tags) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String value = tag.trim();
            if (value.contains("\n") || value.contains("\r")) {
                throw BusinessException.badRequest("服务标签不能包含换行符");
            }
            normalized.add(value);
        }
        return normalized.isEmpty() ? null : String.join("\n", normalized);
    }

    private List<String> decodeTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        return Arrays.stream(tags.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String normalizeOptional(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String defaultName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname().trim() : "房东";
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean integerBoolean(Integer value) {
        return Integer.valueOf(1).equals(value);
    }
}
