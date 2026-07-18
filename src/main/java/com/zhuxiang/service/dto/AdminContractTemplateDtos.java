package com.zhuxiang.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminContractTemplateDtos {
    private AdminContractTemplateDtos() {}

    public record CreateRequest(
            @NotBlank String businessType,
            @NotBlank String templateCode,
            @NotBlank String templateName,
            @NotBlank String environment,
            String versionNote
    ) {}

    public record Summary(
            String id, String businessType, String templateCode, String templateName,
            Integer version, String environment, String docTemplateId, String status,
            long componentCount, long mappedCount, long requiredUnmappedCount,
            LocalDateTime lastSyncedAt, LocalDateTime publishedAt, LocalDateTime updatedAt
    ) {}

    public record Detail(
            String id, String businessType, String templateCode, String templateName,
            Integer version, String environment, String docTemplateId, String status,
            long componentCount, long mappedCount, long requiredUnmappedCount,
            LocalDateTime lastSyncedAt, LocalDateTime publishedAt, LocalDateTime updatedAt,
            String sourceFileId, String sourceFileName, Integer templateType,
            String componentFingerprint, Long esignCreateTime, Long esignUpdateTime,
            String validationStatus, String validationMessage,
            String createdByName, String publishedByName
    ) {}

    public record ComponentView(
            String id, String componentId, String componentKey, String componentName,
            Integer componentType, boolean required, Integer pageNum,
            BigDecimal positionX, BigDecimal positionY, BigDecimal width, BigDecimal height,
            String signerRole, String mappingMode, String businessFieldCode,
            String fixedValue, boolean editable, String syncStatus,
            List<String> validationErrors
    ) {}

    public record FieldDefinition(
            String fieldCode, String displayName, String category, String valueType,
            List<Integer> supportedComponentTypes, boolean sensitive, String description
    ) {}

    public record MappingItem(
            @NotBlank String componentId,
            @NotBlank String mappingMode,
            String businessFieldCode,
            String fixedValue,
            Boolean editable
    ) {}

    public record SaveMappingsRequest(@NotEmpty List<@Valid MappingItem> mappings) {}

    public record ValidationIssue(
            String level, String code, String message,
            String componentId, String componentKey
    ) {}

    public record ValidationResult(
            boolean passed, long errorCount, long warningCount,
            long componentCount, long mappedCount,
            List<ValidationIssue> issues, LocalDateTime validatedAt
    ) {}

    public record PageLink(
            String url, String longUrl, LocalDateTime expiresAt, String docTemplateId
    ) {}

    public record Preview(
            String fileUrl, String fileName,
            BigDecimal pageWidth, BigDecimal pageHeight,
            LocalDateTime expiresAt
    ) {}

    public record AuditLog(
            String id, String action, String operatorName,
            String detail, LocalDateTime createdAt
    ) {}
}
