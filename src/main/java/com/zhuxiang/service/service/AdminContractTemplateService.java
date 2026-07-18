package com.zhuxiang.service.service;

import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminContractTemplateDtos;
import com.zhuxiang.service.dto.LeaseContractFillData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminContractTemplateService {
    PageData<AdminContractTemplateDtos.Summary> list(long page, long pageSize, String keyword, String status);
    AdminContractTemplateDtos.Detail get(String templateId);
    AdminContractTemplateDtos.Detail create(AdminContractTemplateDtos.CreateRequest request, String operatorId);
    AdminContractTemplateDtos.Detail uploadSource(String templateId, MultipartFile file, String operatorId);
    AdminContractTemplateDtos.PageLink createUrl(String templateId, String operatorId);
    AdminContractTemplateDtos.PageLink editUrl(String templateId, String operatorId);
    List<AdminContractTemplateDtos.ComponentView> sync(String templateId, String operatorId);
    List<AdminContractTemplateDtos.ComponentView> components(String templateId);
    List<AdminContractTemplateDtos.FieldDefinition> fieldDefinitions();
    List<AdminContractTemplateDtos.ComponentView> saveMappings(
            String templateId, AdminContractTemplateDtos.SaveMappingsRequest request, String operatorId);
    AdminContractTemplateDtos.ValidationResult validate(String templateId, String operatorId);
    AdminContractTemplateDtos.Preview generateTestFile(String templateId, String operatorId);
    AdminContractTemplateDtos.Detail publish(String templateId, String operatorId);
    AdminContractTemplateDtos.Detail offline(String templateId, String operatorId);
    AdminContractTemplateDtos.Detail cloneVersion(String templateId, String operatorId);
    AdminContractTemplateDtos.Preview preview(String templateId);
    List<AdminContractTemplateDtos.AuditLog> auditLogs(String templateId);

    RuntimeTemplate resolveRuntimeTemplate(String templateConfigId, LeaseContractFillData fillData);
    RuntimeTemplate resolveActiveRuntimeTemplate(LeaseContractFillData fillData);

    record SignaturePosition(int page, double x, double y) {}
    record RuntimeTemplate(
            String configId, Integer version, String docTemplateId, String fingerprint,
            List<com.zhuxiang.service.client.EsignV3Client.Component> components,
            SignaturePosition lessorSignature, SignaturePosition tenantSignature
    ) {}
}
