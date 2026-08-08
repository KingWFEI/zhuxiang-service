package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhuxiang.service.entity.UserRealNameAuth;
import com.zhuxiang.service.mapper.UserRealNameAuthMapper;
import com.zhuxiang.service.service.impl.RealNameVerificationQueryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealNameVerificationQueryServiceTests {

    private final UserRealNameAuthMapper mapper = mock(UserRealNameAuthMapper.class);
    private final RealNameVerificationQueryServiceImpl service =
            new RealNameVerificationQueryServiceImpl(mapper);

    @Test
    @SuppressWarnings("unchecked")
    void verifiedStateComesFromVerifiedAuthRecord() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThat(service.isVerified("user-1")).isTrue();
    }

    @Test
    void blankUserIdIsNeverVerified() {
        assertThat(service.isVerified(" ")).isFalse();
        verify(mapper, never()).selectCount(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchLookupReturnsDistinctVerifiedUserIds() {
        UserRealNameAuth first = new UserRealNameAuth();
        first.setUserId("user-1");
        UserRealNameAuth duplicate = new UserRealNameAuth();
        duplicate.setUserId("user-1");
        when(mapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(first, duplicate));

        assertThat(service.findVerifiedUserIds(List.of("user-1", "user-2")))
                .isEqualTo(Set.of("user-1"));
    }
}
