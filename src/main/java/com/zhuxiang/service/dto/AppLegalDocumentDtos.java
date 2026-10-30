package com.zhuxiang.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public final class AppLegalDocumentDtos {

    private AppLegalDocumentDtos() {
    }

    @Schema(description = "APP 协议或隐私政策")
    public record DocumentView(
            @Schema(description = "文档类型", example = "USER_AGREEMENT")
            String documentType,
            @Schema(description = "文档标题", example = "用户协议")
            String title,
            @Schema(description = "Markdown 正文")
            String contentMarkdown,
            @Schema(description = "最后更新时间")
            LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "保存 APP 协议或隐私政策")
    public record SaveDocumentRequest(
            @NotBlank(message = "Markdown 内容不能为空")
            @Size(max = 2_000_000, message = "Markdown 内容不能超过 200 万字符")
            @Schema(description = "Markdown 正文")
            String contentMarkdown
    ) {
    }
}
