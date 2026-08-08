package com.zhuxiang.service.service;

import java.util.Collection;
import java.util.Set;

/**
 * Reads the authoritative real-name verification state from user_real_name_auth.
 */
public interface RealNameVerificationQueryService {

    boolean isVerified(String userId);

    Set<String> findVerifiedUserIds(Collection<String> userIds);
}
