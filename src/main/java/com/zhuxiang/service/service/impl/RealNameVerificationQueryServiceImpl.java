package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhuxiang.service.entity.UserRealNameAuth;
import com.zhuxiang.service.enums.RealNameAuthStatus;
import com.zhuxiang.service.mapper.UserRealNameAuthMapper;
import com.zhuxiang.service.service.RealNameVerificationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RealNameVerificationQueryServiceImpl implements RealNameVerificationQueryService {

    private final UserRealNameAuthMapper mapper;

    public RealNameVerificationQueryServiceImpl(UserRealNameAuthMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isVerified(String userId) {
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        Long count = mapper.selectCount(new LambdaQueryWrapper<UserRealNameAuth>()
                .eq(UserRealNameAuth::getUserId, userId)
                .eq(UserRealNameAuth::getAuthStatus, RealNameAuthStatus.VERIFIED.getValue()));
        return count != null && count > 0;
    }

    @Override
    public Set<String> findVerifiedUserIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        List<String> normalizedIds = userIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            return Set.of();
        }
        List<UserRealNameAuth> records = mapper.selectList(
                new QueryWrapper<UserRealNameAuth>()
                        .select("user_id")
                        .in("user_id", normalizedIds)
                        .eq("auth_status", RealNameAuthStatus.VERIFIED.getValue())
        );
        Set<String> result = new LinkedHashSet<>();
        for (UserRealNameAuth record : records) {
            if (StringUtils.hasText(record.getUserId())) {
                result.add(record.getUserId());
            }
        }
        return Set.copyOf(result);
    }
}
