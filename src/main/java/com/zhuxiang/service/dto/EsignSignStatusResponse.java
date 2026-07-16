package com.zhuxiang.service.dto;

import java.time.LocalDateTime;

/**
 * e签宝签署状态刷新响应。
 */
public record EsignSignStatusResponse(
        String contractStatus,
        boolean lessorSigned,
        boolean tenantSigned,
        boolean currentUserSigned,
        boolean downloadAvailable,
        LocalDateTime signedAt
) {}
