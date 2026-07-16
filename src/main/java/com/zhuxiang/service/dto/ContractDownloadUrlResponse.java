package com.zhuxiang.service.dto;

/**
 * 已签合同下载链接响应。
 */
public record ContractDownloadUrlResponse(
        String fileName,
        String downloadUrl,
        String certificateDownloadUrl
) {}
