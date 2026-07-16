package com.zhuxiang.service.dto;

public record EsignSignResponse(
        String contractStatus,
        String currentUserRole,
        boolean currentUserSigned,
        String signUrl
) {}
