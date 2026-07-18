package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminContractTemplateDtos;
import com.zhuxiang.service.dto.LeaseContractFillData;
import com.zhuxiang.service.entity.EsignContractTemplate;
import com.zhuxiang.service.entity.EsignTemplateAuditLog;
import com.zhuxiang.service.entity.EsignTemplateComponent;
import com.zhuxiang.service.mapper.EsignContractTemplateMapper;
import com.zhuxiang.service.mapper.EsignTemplateAuditLogMapper;
import com.zhuxiang.service.mapper.EsignTemplateComponentMapper;
import com.zhuxiang.service.service.AdminContractTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminContractTemplateServiceImpl implements AdminContractTemplateService {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final long MAX_PDF_SIZE = 50L * 1024 * 1024;

    private final EsignContractTemplateMapper templateMapper;
    private final EsignTemplateComponentMapper componentMapper;
    private final EsignTemplateAuditLogMapper auditMapper;
    private final EsignV3Client esignClient;
    private final ObjectMapper objectMapper;

    public AdminContractTemplateServiceImpl(EsignContractTemplateMapper templateMapper,
                                            EsignTemplateComponentMapper componentMapper,
                                            EsignTemplateAuditLogMapper auditMapper,
                                            EsignV3Client esignClient, ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.componentMapper = componentMapper;
        this.auditMapper = auditMapper;
        this.esignClient = esignClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageData<AdminContractTemplateDtos.Summary> list(long page, long pageSize, String keyword, String status) {
        LambdaQueryWrapper<EsignContractTemplate> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) q.and(w -> w.like(EsignContractTemplate::getTemplateName, keyword)
                .or().like(EsignContractTemplate::getTemplateCode, keyword)
                .or().like(EsignContractTemplate::getDocTemplateId, keyword));
        if (StringUtils.hasText(status)) q.eq(EsignContractTemplate::getStatus, status);
        q.orderByDesc(EsignContractTemplate::getUpdatedAt);
        Page<EsignContractTemplate> result = templateMapper.selectPage(new Page<>(page, pageSize), q);
        return PageData.of(result.getRecords().stream().map(this::summary).toList(), page, pageSize, result.getTotal());
    }

    @Override public AdminContractTemplateDtos.Detail get(String templateId) { return detail(requireTemplate(templateId)); }

    @Override
    @Transactional
    public AdminContractTemplateDtos.Detail create(AdminContractTemplateDtos.CreateRequest r, String operatorId) {
        Integer maxVersion = templateMapper.selectList(new LambdaQueryWrapper<EsignContractTemplate>()
                        .eq(EsignContractTemplate::getBusinessType, r.businessType())
                        .eq(EsignContractTemplate::getTemplateCode, r.templateCode()))
                .stream().map(EsignContractTemplate::getVersion).max(Integer::compareTo).orElse(0);
        EsignContractTemplate t = new EsignContractTemplate();
        t.setId(UUID.randomUUID().toString());
        t.setBusinessType(r.businessType()); t.setTemplateCode(r.templateCode()); t.setTemplateName(r.templateName());
        t.setVersion(maxVersion + 1); t.setEnvironment(r.environment()); t.setTemplateType(1);
        t.setStatus("DRAFT"); t.setValidationStatus("NOT_VALIDATED"); t.setVersionNote(r.versionNote());
        t.setCreatedBy(operatorId); t.setCreatedAt(LocalDateTime.now()); t.setUpdatedAt(t.getCreatedAt()); t.setVersionLock(0);
        templateMapper.insert(t); audit(t.getId(), "CREATE", operatorId, "创建合同模板版本 V" + t.getVersion());
        return detail(t);
    }

    @Override
    @Transactional
    public AdminContractTemplateDtos.Detail uploadSource(String templateId, MultipartFile file, String operatorId) {
        EsignContractTemplate t = editable(templateId);
        if (file == null || file.isEmpty()) throw BusinessException.badRequest("请选择合同 PDF 文件");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("contract.pdf");
        if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf")) throw BusinessException.badRequest("合同源文件必须是 PDF");
        if (file.getSize() > MAX_PDF_SIZE) throw BusinessException.badRequest("合同 PDF 不能超过 50MB");
        try {
            EsignV3Client.FileUploadResult uploaded = esignClient.uploadLocalFile(file.getBytes(), name, "application/pdf");
            t.setSourceFileId(uploaded.fileId()); t.setSourceFileName(name); t.setDocTemplateId(null);
            resetAfterChange(t); templateMapper.updateById(t);
            audit(t.getId(), "UPLOAD_SOURCE", operatorId, "上传合同源文件：" + name);
            return detail(t);
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw BusinessException.badRequest("读取合同 PDF 失败"); }
    }

    @Override
    @Transactional
    public AdminContractTemplateDtos.PageLink createUrl(String templateId, String operatorId) {
        EsignContractTemplate t = editable(templateId);
        if (!StringUtils.hasText(t.getSourceFileId())) throw BusinessException.badRequest("请先上传合同 PDF");
        EsignV3Client.FileStatusResponse status = esignClient.getFileStatus(t.getSourceFileId());
        Integer fileStatus = status.getData() == null ? null : status.getData().getFileStatus();
        if (!Objects.equals(fileStatus, 2) && !Objects.equals(fileStatus, 5)) {
            throw BusinessException.badRequest("合同 PDF 仍在e签宝处理中，请稍后重试");
        }
        EsignV3Client.TemplatePageResponse response = esignClient.getCreateTemplateUrl(t.getSourceFileId(), t.getTemplateName());
        if (response.getData() == null) throw BusinessException.badRequest("e签宝未返回模板制作地址");
        if (StringUtils.hasText(response.getData().getDocTemplateId())) t.setDocTemplateId(response.getData().getDocTemplateId());
        t.setUpdatedAt(LocalDateTime.now()); templateMapper.updateById(t);
        audit(t.getId(), "OPEN_CREATE_PAGE", operatorId, "打开e签宝模板制作页");
        return pageLink(response, t.getDocTemplateId());
    }

    @Override
    public AdminContractTemplateDtos.PageLink editUrl(String templateId, String operatorId) {
        EsignContractTemplate t = editable(templateId); requireDocTemplate(t);
        EsignV3Client.TemplatePageResponse response = esignClient.getEditTemplateUrl(t.getDocTemplateId());
        audit(t.getId(), "OPEN_EDIT_PAGE", operatorId, "打开e签宝模板编辑页");
        return pageLink(response, t.getDocTemplateId());
    }

    @Override
    @Transactional
    public List<AdminContractTemplateDtos.ComponentView> sync(String templateId, String operatorId) {
        EsignContractTemplate t = editable(templateId); requireDocTemplate(t);
        EsignV3Client.TemplateDetailResponse response = esignClient.getTemplateDetail(t.getDocTemplateId());
        if (response.getCode() != 0 || response.getData() == null) throw BusinessException.badRequest("获取e签宝模板控件失败：" + response.getMessage());
        List<EsignV3Client.TemplateDetailResponse.StructComponent> remote = Optional.ofNullable(response.getData().getComponents()).orElse(List.of());
        Map<String, EsignTemplateComponent> old = rawComponents(templateId).stream()
                .collect(Collectors.toMap(EsignTemplateComponent::getComponentId, Function.identity(), (a, b) -> a));
        Set<String> seen = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (var source : remote) {
            seen.add(source.getComponentId());
            EsignTemplateComponent c = old.getOrDefault(source.getComponentId(), new EsignTemplateComponent());
            boolean added = c.getId() == null;
            if (added) { c.setId(UUID.randomUUID().toString()); c.setTemplateId(templateId); c.setComponentId(source.getComponentId()); c.setCreatedAt(now); }
            c.setComponentKey(source.getComponentKey()); c.setComponentName(source.getComponentName());
            c.setComponentType(source.getComponentType()); c.setRequiredFlag(source.isRequired() ? 1 : 0);
            if (source.getComponentPosition() != null) {
                c.setPageNum(source.getComponentPosition().getPageNum()); c.setPositionX(source.getComponentPosition().getX()); c.setPositionY(source.getComponentPosition().getY());
            }
            if (source.getComponentSize() != null) { c.setComponentWidth(source.getComponentSize().getWidth()); c.setComponentHeight(source.getComponentSize().getHeight()); }
            if (source.getComponentSpecialAttribute() != null) c.setSignerRole(source.getComponentSpecialAttribute().getSignerRole());
            try { c.setSpecialAttribute(objectMapper.writeValueAsString(source.getComponentSpecialAttribute())); } catch (Exception ignored) { c.setSpecialAttribute(null); }
            if (added && source.getComponentType() == 6) c.setMappingMode("SIGNATURE");
            if (added && source.getComponentType() == 23) c.setMappingMode("IGNORE");
            c.setSyncStatus(added ? "NEW" : "NORMAL"); c.setUpdatedAt(now);
            if (added) componentMapper.insert(c); else componentMapper.updateById(c);
        }
        old.values().stream().filter(c -> !seen.contains(c.getComponentId())).forEach(c -> {
            c.setSyncStatus("REMOVED"); c.setUpdatedAt(now); componentMapper.updateById(c);
        });
        t.setComponentFingerprint(fingerprint(remote)); t.setEsignCreateTime(response.getData().getCreateTime());
        t.setEsignUpdateTime(response.getData().getUpdateTime()); t.setLastSyncedAt(now);
        resetAfterChange(t); templateMapper.updateById(t);
        audit(templateId, "SYNC_COMPONENTS", operatorId, "同步控件，共 " + remote.size() + " 个");
        return components(templateId);
    }

    @Override public List<AdminContractTemplateDtos.ComponentView> components(String templateId) {
        requireTemplate(templateId); return rawComponents(templateId).stream().map(c -> componentView(c, List.of())).toList();
    }

    @Override public List<AdminContractTemplateDtos.FieldDefinition> fieldDefinitions() { return FIELDS; }

    @Override
    @Transactional
    public List<AdminContractTemplateDtos.ComponentView> saveMappings(String templateId,
            AdminContractTemplateDtos.SaveMappingsRequest request, String operatorId) {
        EsignContractTemplate t = editable(templateId);
        Map<String, EsignTemplateComponent> byId = rawComponents(templateId).stream()
                .collect(Collectors.toMap(EsignTemplateComponent::getComponentId, Function.identity()));
        for (var item : request.mappings()) {
            EsignTemplateComponent c = byId.get(item.componentId());
            if (c == null) throw BusinessException.badRequest("控件不属于当前模板：" + item.componentId());
            c.setMappingMode(item.mappingMode()); c.setBusinessFieldCode(blankToNull(item.businessFieldCode()));
            c.setFixedValue(item.fixedValue()); c.setEditableFlag(Boolean.TRUE.equals(item.editable()) ? 1 : 0);
            c.setUpdatedAt(LocalDateTime.now()); componentMapper.updateById(c);
        }
        resetAfterChange(t); templateMapper.updateById(t);
        audit(templateId, "SAVE_MAPPINGS", operatorId, "保存控件映射配置");
        return components(templateId);
    }

    @Override
    @Transactional
    public AdminContractTemplateDtos.ValidationResult validate(String templateId, String operatorId) {
        EsignContractTemplate t = requireTemplate(templateId);
        List<EsignTemplateComponent> components = activeComponents(templateId);
        List<AdminContractTemplateDtos.ValidationIssue> issues = validateComponents(t, components);
        long errors = issues.stream().filter(i -> "ERROR".equals(i.level())).count();
        long warnings = issues.stream().filter(i -> "WARNING".equals(i.level())).count();
        long mapped = components.stream().filter(this::mapped).count();
        t.setValidationStatus(errors == 0 ? "PASSED" : "FAILED");
        t.setValidationMessage(toJson(issues)); t.setUpdatedAt(LocalDateTime.now()); templateMapper.updateById(t);
        audit(templateId, "VALIDATE", operatorId, errors == 0 ? "模板校验通过" : "模板校验失败，错误 " + errors + " 项");
        return new AdminContractTemplateDtos.ValidationResult(errors == 0, errors, warnings,
                components.size(), mapped, issues, LocalDateTime.now());
    }

    @Override
    public AdminContractTemplateDtos.Preview generateTestFile(String templateId, String operatorId) {
        LeaseContractFillData sample = LeaseContractFillData.builder().lessorName("测试房东").lessorMobile("13800000001")
                .lessorIdCard("110101199001011234").tenantName("测试租户").tenantMobile("13800000002")
                .tenantIdCard("110101199202023456").houseAddress("测试市示例区幸福路1号101室")
                .leaseMonths(12).leaseStartDate(LocalDate.now()).leaseEndDate(LocalDate.now().plusYears(1))
                .noticeMonths(1).monthlyRent(new BigDecimal("350000")).deposit(new BigDecimal("350000"))
                .rentPaymentDate(LocalDate.now()).lessorSignDate(LocalDate.now()).tenantSignDate(LocalDate.now()).build();
        RuntimeTemplate runtime = resolveRuntimeTemplate(templateId, sample);
        EsignV3Client.CreateFileResponse response = esignClient.createByDocTemplateComponents(runtime.components(),
                runtime.docTemplateId(), "合同模板测试-" + System.currentTimeMillis() + ".pdf");
        audit(templateId, "GENERATE_TEST", operatorId, "生成测试合同文件");
        String url = response.getData() == null ? null : response.getData().getFileDownloadUrl();
        return new AdminContractTemplateDtos.Preview(url, "合同模板测试.pdf", null, null, LocalDateTime.now().plusHours(1));
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public AdminContractTemplateDtos.Detail publish(String templateId, String operatorId) {
        EsignContractTemplate t = editable(templateId);
        requireDocTemplate(t);
        EsignV3Client.TemplateDetailResponse remote = esignClient.getTemplateDetail(t.getDocTemplateId());
        String currentFingerprint = fingerprint(remote.getData() == null ? List.of() : remote.getData().getComponents());
        if (!Objects.equals(t.getComponentFingerprint(), currentFingerprint)) {
            t.setStatus("DRIFTED"); t.setValidationStatus("FAILED"); t.setValidationMessage("发布前检测到e签宝模板控件发生变化");
            t.setUpdatedAt(LocalDateTime.now()); templateMapper.updateById(t);
            audit(templateId, "DRIFT_DETECTED", operatorId, "发布前检测到模板变化");
            throw BusinessException.conflict("e签宝模板已变化，请先重新同步控件");
        }
        AdminContractTemplateDtos.ValidationResult result = validate(templateId, operatorId);
        if (!result.passed()) throw BusinessException.badRequest("模板校验未通过，不能发布");
        templateMapper.selectList(new LambdaQueryWrapper<EsignContractTemplate>()
                .eq(EsignContractTemplate::getBusinessType, t.getBusinessType()).eq(EsignContractTemplate::getStatus, "ACTIVE"))
                .forEach(old -> { old.setStatus("INACTIVE"); old.setUpdatedAt(LocalDateTime.now()); templateMapper.updateById(old); });
        t.setStatus("ACTIVE"); t.setPublishedBy(operatorId); t.setPublishedAt(LocalDateTime.now()); t.setUpdatedAt(t.getPublishedAt());
        templateMapper.updateById(t); audit(templateId, "PUBLISH", operatorId, "发布模板 V" + t.getVersion());
        return detail(t);
    }

    @Override
    @Transactional
    public AdminContractTemplateDtos.Detail offline(String templateId, String operatorId) {
        EsignContractTemplate t = requireTemplate(templateId);
        if (!"ACTIVE".equals(t.getStatus())) throw BusinessException.badRequest("只有已发布模板可以下线");
        t.setStatus("INACTIVE"); t.setUpdatedAt(LocalDateTime.now()); templateMapper.updateById(t);
        audit(templateId, "OFFLINE", operatorId, "下线模板"); return detail(t);
    }

    @Override
    @Transactional
    public AdminContractTemplateDtos.Detail cloneVersion(String templateId, String operatorId) {
        EsignContractTemplate source = requireTemplate(templateId);
        AdminContractTemplateDtos.CreateRequest request = new AdminContractTemplateDtos.CreateRequest(source.getBusinessType(),
                source.getTemplateCode(), source.getTemplateName(), source.getEnvironment(), "基于 V" + source.getVersion() + " 创建");
        AdminContractTemplateDtos.Detail created = create(request, operatorId);
        EsignContractTemplate target = requireTemplate(created.id());
        target.setSourceFileId(source.getSourceFileId()); target.setSourceFileName(source.getSourceFileName()); templateMapper.updateById(target);
        audit(target.getId(), "CLONE_VERSION", operatorId, "从模板 " + source.getId() + " 克隆"); return detail(target);
    }

    @Override
    public AdminContractTemplateDtos.Preview preview(String templateId) {
        EsignContractTemplate t = requireTemplate(templateId); requireDocTemplate(t);
        EsignV3Client.TemplateDetailResponse response = esignClient.getTemplateDetail(t.getDocTemplateId());
        String url = response.getData() == null ? null : response.getData().getFileDownloadUrl();
        return new AdminContractTemplateDtos.Preview(url, t.getSourceFileName(), null, null, LocalDateTime.now().plusHours(1));
    }

    @Override public List<AdminContractTemplateDtos.AuditLog> auditLogs(String templateId) {
        requireTemplate(templateId);
        return auditMapper.selectList(new LambdaQueryWrapper<EsignTemplateAuditLog>()
                        .eq(EsignTemplateAuditLog::getTemplateId, templateId).orderByDesc(EsignTemplateAuditLog::getCreatedAt))
                .stream().map(a -> new AdminContractTemplateDtos.AuditLog(a.getId(), a.getAction(), a.getOperatorName(), a.getDetailText(), a.getCreatedAt())).toList();
    }

    @Override public RuntimeTemplate resolveActiveRuntimeTemplate(LeaseContractFillData fillData) {
        EsignContractTemplate t = templateMapper.selectOne(new LambdaQueryWrapper<EsignContractTemplate>()
                .eq(EsignContractTemplate::getBusinessType, "HOUSE_LEASE")
                .in(EsignContractTemplate::getStatus, List.of("ACTIVE", "DRIFTED"))
                .orderByDesc(EsignContractTemplate::getVersion).last("LIMIT 1"));
        if (t == null) throw BusinessException.notFound("尚未发布可用的租房合同模板");
        if ("DRIFTED".equals(t.getStatus())) throw BusinessException.conflict("已发布合同模板发生变化，请管理员重新同步、校验并发布");
        EsignV3Client.TemplateDetailResponse remote = esignClient.getTemplateDetail(t.getDocTemplateId());
        String currentFingerprint = fingerprint(remote.getData() == null ? List.of() : remote.getData().getComponents());
        if (!Objects.equals(t.getComponentFingerprint(), currentFingerprint)) {
            t.setStatus("DRIFTED"); t.setValidationStatus("FAILED");
            t.setValidationMessage("e签宝模板控件已在发布后发生变化"); t.setUpdatedAt(LocalDateTime.now());
            templateMapper.updateById(t);
            audit(t.getId(), "DRIFT_DETECTED", null, "签约前检测到e签宝模板控件发生变化");
            throw BusinessException.conflict("合同模板已被修改，请管理员重新同步、校验并发布后再签约");
        }
        return resolveRuntimeTemplate(t.getId(), fillData);
    }

    @Override public RuntimeTemplate resolveRuntimeTemplate(String templateConfigId, LeaseContractFillData fillData) {
        EsignContractTemplate t = requireTemplate(templateConfigId); requireDocTemplate(t);
        List<EsignTemplateComponent> components = activeComponents(templateConfigId);
        List<EsignV3Client.Component> values = new ArrayList<>();
        SignaturePosition lessor = null, tenant = null;
        for (EsignTemplateComponent c : components) {
            if ("SIGNATURE".equals(c.getMappingMode())) {
                SignaturePosition p = new SignaturePosition(nvl(c.getPageNum(), 1), decimal(c.getPositionX()), decimal(c.getPositionY()));
                if (isLessor(c.getSignerRole())) lessor = p; else if (isTenant(c.getSignerRole())) tenant = p;
                continue;
            }
            if ("IGNORE".equals(c.getMappingMode()) || !StringUtils.hasText(c.getMappingMode())) continue;
            if (!StringUtils.hasText(c.getComponentKey())) throw BusinessException.badRequest("模板控件未设置 componentKey：" + c.getComponentName());
            String value = "FIXED_VALUE".equals(c.getMappingMode()) ? c.getFixedValue() : resolveField(c.getBusinessFieldCode(), fillData, c);
            if (c.getRequiredFlag() == 1 && !StringUtils.hasText(value)) throw BusinessException.badRequest("合同必填字段缺少数据：" + c.getComponentName());
            validateComponentValueLength(c, value);
            values.add(esignClient.keyedComponent(c.getComponentKey(), value));
        }
        return new RuntimeTemplate(t.getId(), t.getVersion(), t.getDocTemplateId(), t.getComponentFingerprint(), values, lessor, tenant);
    }

    private List<AdminContractTemplateDtos.ValidationIssue> validateComponents(EsignContractTemplate t, List<EsignTemplateComponent> list) {
        List<AdminContractTemplateDtos.ValidationIssue> out = new ArrayList<>();
        if (!StringUtils.hasText(t.getDocTemplateId())) out.add(issue("ERROR", "NO_ESIGN_TEMPLATE", "尚未创建e签宝模板", null));
        if (list.isEmpty()) out.add(issue("ERROR", "NO_COMPONENTS", "模板尚未同步控件", null));
        Set<String> keys = new HashSet<>(); int lessorSigns = 0, tenantSigns = 0;
        Map<String, AdminContractTemplateDtos.FieldDefinition> fieldDefs = FIELDS.stream()
                .collect(Collectors.toMap(AdminContractTemplateDtos.FieldDefinition::fieldCode, Function.identity()));
        for (EsignTemplateComponent c : list) {
            if (StringUtils.hasText(c.getComponentKey()) && !keys.add(c.getComponentKey())) out.add(issue("ERROR", "DUPLICATE_KEY", "componentKey 重复", c));
            String mode = c.getMappingMode();
            if (c.getRequiredFlag() == 1 && !StringUtils.hasText(mode)) out.add(issue("ERROR", "REQUIRED_UNMAPPED", "必填控件尚未配置映射", c));
            if (("SYSTEM_FIELD".equals(mode) || "DERIVED".equals(mode)) && !fieldDefs.containsKey(c.getBusinessFieldCode())) out.add(issue("ERROR", "UNKNOWN_FIELD", "业务字段无效", c));
            if (("SYSTEM_FIELD".equals(mode) || "DERIVED".equals(mode)) && fieldDefs.containsKey(c.getBusinessFieldCode())
                    && c.getComponentType() != null && !fieldDefs.get(c.getBusinessFieldCode()).supportedComponentTypes().contains(c.getComponentType()))
                out.add(issue("WARNING", "COMPONENT_TYPE_MISMATCH", "控件类型与业务字段类型可能不兼容", c));
            if ("FIXED_VALUE".equals(mode) && !StringUtils.hasText(c.getFixedValue())) out.add(issue("ERROR", "EMPTY_FIXED_VALUE", "固定值不能为空", c));
            if ("USER_INPUT".equals(mode)) out.add(issue("ERROR", "USER_INPUT_UNSUPPORTED", "当前App签约流程不支持签前人工填写，请改用系统字段或固定值", c));
            if (("SYSTEM_FIELD".equals(mode) || "DERIVED".equals(mode) || "FIXED_VALUE".equals(mode)) && !StringUtils.hasText(c.getComponentKey())) out.add(issue("ERROR", "MISSING_COMPONENT_KEY", "请在e签宝模板中为控件设置唯一 componentKey", c));
            if ("SIGNATURE".equals(mode)) {
                if (c.getPageNum() == null || c.getPositionX() == null || c.getPositionY() == null) out.add(issue("ERROR", "SIGN_POSITION_MISSING", "签章控件缺少页码或坐标", c));
                if (isLessor(c.getSignerRole())) lessorSigns++; else if (isTenant(c.getSignerRole())) tenantSigns++;
                else out.add(issue("ERROR", "SIGNER_ROLE_INVALID", "签章控件角色必须为甲方或乙方", c));
            }
        }
        if (lessorSigns != 1) out.add(issue("ERROR", "LESSOR_SIGN_COUNT", "甲方签章控件必须且只能有一个", null));
        if (tenantSigns != 1) out.add(issue("ERROR", "TENANT_SIGN_COUNT", "乙方签章控件必须且只能有一个", null));
        return out;
    }

    private String resolveField(String code, LeaseContractFillData d, EsignTemplateComponent c) {
        if (code == null) return null;
        return switch (code) {
            case "LESSOR_REAL_NAME" -> d.getLessorName(); case "LESSOR_ID_CARD" -> d.getLessorIdCard(); case "LESSOR_MOBILE" -> d.getLessorMobile();
            case "TENANT_REAL_NAME" -> d.getTenantName(); case "TENANT_ID_CARD" -> d.getTenantIdCard(); case "TENANT_MOBILE" -> d.getTenantMobile();
            case "HOUSE_FULL_ADDRESS" -> d.getHouseAddress(); case "LEASE_YEARS" -> leaseYears(d.getLeaseMonths());
            case "LEASE_START_DATE" -> componentDate(d.getLeaseStartDate(), c); case "LEASE_END_DATE" -> componentDate(d.getLeaseEndDate(), c);
            case "NOTICE_MONTHS" -> String.valueOf(nvl(d.getNoticeMonths(), 1)); case "MONTHLY_RENT" -> yuan(d.getMonthlyRent());
            case "DEPOSIT" -> yuan(d.getDeposit()); case "RENT_PAYMENT_DATE" -> componentDate(d.getRentPaymentDate(), c);
            case "LESSOR_SIGN_DATE" -> componentDate(Optional.ofNullable(d.getLessorSignDate()).orElse(LocalDate.now()), c);
            case "TENANT_SIGN_DATE" -> componentDate(Optional.ofNullable(d.getTenantSignDate()).orElse(LocalDate.now()), c);
            default -> null;
        };
    }

    private static final List<AdminContractTemplateDtos.FieldDefinition> FIELDS = List.of(
            field("LESSOR_REAL_NAME", "出租方姓名", "甲方", "STRING", true), field("LESSOR_ID_CARD", "出租方身份证号", "甲方", "STRING", true), field("LESSOR_MOBILE", "出租方手机号", "甲方", "STRING", true),
            field("TENANT_REAL_NAME", "承租方姓名", "乙方", "STRING", true), field("TENANT_ID_CARD", "承租方身份证号", "乙方", "STRING", true), field("TENANT_MOBILE", "承租方手机号", "乙方", "STRING", true),
            field("HOUSE_FULL_ADDRESS", "房源完整地址", "房源", "STRING", false), field("LEASE_YEARS", "租期（年）", "租约", "NUMBER", false),
            field("LEASE_START_DATE", "起租日期", "租约", "DATE", false), field("LEASE_END_DATE", "截止日期", "租约", "DATE", false), field("NOTICE_MONTHS", "提前通知月数", "租约", "NUMBER", false),
            field("MONTHLY_RENT", "月租金（元）", "费用", "NUMBER", false), field("DEPOSIT", "押金（元）", "费用", "NUMBER", false), field("RENT_PAYMENT_DATE", "租金交付日", "费用", "DATE", false),
            field("LESSOR_SIGN_DATE", "出租方签署日期", "签署", "DATE", false), field("TENANT_SIGN_DATE", "承租方签署日期", "签署", "DATE", false));

    private static AdminContractTemplateDtos.FieldDefinition field(String code, String name, String category, String type, boolean sensitive) {
        List<Integer> supported = switch (type) {
            case "DATE" -> List.of(1, 3);
            case "NUMBER" -> List.of(1, 2);
            default -> List.of(1, 8, 16, 19);
        };
        return new AdminContractTemplateDtos.FieldDefinition(code, name, category, type, supported, sensitive, name);
    }

    private EsignContractTemplate requireTemplate(String id) { EsignContractTemplate t = templateMapper.selectById(id); if (t == null) throw BusinessException.notFound("合同模板不存在"); return t; }
    private EsignContractTemplate editable(String id) { EsignContractTemplate t = requireTemplate(id); if ("ACTIVE".equals(t.getStatus())) throw BusinessException.badRequest("已发布模板不可直接修改，请创建新版本"); return t; }
    private void requireDocTemplate(EsignContractTemplate t) { if (!StringUtils.hasText(t.getDocTemplateId())) throw BusinessException.badRequest("尚未创建e签宝模板"); }
    private List<EsignTemplateComponent> rawComponents(String id) { return componentMapper.selectList(new LambdaQueryWrapper<EsignTemplateComponent>().eq(EsignTemplateComponent::getTemplateId, id).orderByAsc(EsignTemplateComponent::getPageNum).orderByAsc(EsignTemplateComponent::getCreatedAt)); }
    private List<EsignTemplateComponent> activeComponents(String id) { return rawComponents(id).stream().filter(c -> !"REMOVED".equals(c.getSyncStatus())).toList(); }
    private void resetAfterChange(EsignContractTemplate t) { t.setValidationStatus("NOT_VALIDATED"); t.setValidationMessage(null); t.setUpdatedAt(LocalDateTime.now()); }
    private boolean mapped(EsignTemplateComponent c) { return StringUtils.hasText(c.getMappingMode()) && !"IGNORE".equals(c.getMappingMode()); }
    private long count(String id) { return activeComponents(id).size(); }
    private long mappedCount(String id) { return activeComponents(id).stream().filter(this::mapped).count(); }
    private long requiredUnmapped(String id) { return activeComponents(id).stream().filter(c -> c.getRequiredFlag() == 1 && !mapped(c)).count(); }
    private AdminContractTemplateDtos.Summary summary(EsignContractTemplate t) { return new AdminContractTemplateDtos.Summary(t.getId(), t.getBusinessType(), t.getTemplateCode(), t.getTemplateName(), t.getVersion(), t.getEnvironment(), t.getDocTemplateId(), t.getStatus(), count(t.getId()), mappedCount(t.getId()), requiredUnmapped(t.getId()), t.getLastSyncedAt(), t.getPublishedAt(), t.getUpdatedAt()); }
    private AdminContractTemplateDtos.Detail detail(EsignContractTemplate t) { return new AdminContractTemplateDtos.Detail(t.getId(), t.getBusinessType(), t.getTemplateCode(), t.getTemplateName(), t.getVersion(), t.getEnvironment(), t.getDocTemplateId(), t.getStatus(), count(t.getId()), mappedCount(t.getId()), requiredUnmapped(t.getId()), t.getLastSyncedAt(), t.getPublishedAt(), t.getUpdatedAt(), t.getSourceFileId(), t.getSourceFileName(), t.getTemplateType(), t.getComponentFingerprint(), t.getEsignCreateTime(), t.getEsignUpdateTime(), t.getValidationStatus(), t.getValidationMessage(), t.getCreatedBy(), t.getPublishedBy()); }
    private AdminContractTemplateDtos.ComponentView componentView(EsignTemplateComponent c, List<String> errors) { return new AdminContractTemplateDtos.ComponentView(c.getId(), c.getComponentId(), c.getComponentKey(), c.getComponentName(), c.getComponentType(), c.getRequiredFlag() == 1, c.getPageNum(), c.getPositionX(), c.getPositionY(), c.getComponentWidth(), c.getComponentHeight(), c.getSignerRole(), c.getMappingMode(), c.getBusinessFieldCode(), c.getFixedValue(), c.getEditableFlag() == 1, c.getSyncStatus(), errors); }
    private AdminContractTemplateDtos.PageLink pageLink(EsignV3Client.TemplatePageResponse r, String id) { var d = r.getData(); return new AdminContractTemplateDtos.PageLink(d == null ? null : d.getUrl(), d == null ? null : d.getLongUrl(), LocalDateTime.now().plusHours(24), id); }
    private AdminContractTemplateDtos.ValidationIssue issue(String level, String code, String message, EsignTemplateComponent c) { return new AdminContractTemplateDtos.ValidationIssue(level, code, message, c == null ? null : c.getComponentId(), c == null ? null : c.getComponentKey()); }
    private void audit(String templateId, String action, String operator, String detail) { EsignTemplateAuditLog a = new EsignTemplateAuditLog(); a.setId(UUID.randomUUID().toString()); a.setTemplateId(templateId); a.setAction(action); a.setOperatorId(operator); a.setOperatorName(operator); a.setDetailText(detail); a.setCreatedAt(LocalDateTime.now()); auditMapper.insert(a); }
    private String toJson(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception e) { return "[]"; } }
    private String fingerprint(Object value) { try { byte[] hash = MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(value)); return HexFormat.of().formatHex(hash); } catch (Exception e) { return UUID.nameUUIDFromBytes(String.valueOf(value).getBytes(StandardCharsets.UTF_8)).toString().replace("-", ""); } }
    private static String blankToNull(String s) { return StringUtils.hasText(s) ? s : null; }
    private static boolean isLessor(String role) { return "甲方".equals(role) || "出租方".equals(role); }
    private static boolean isTenant(String role) { return "乙方".equals(role) || "承租方".equals(role); }
    private static int nvl(Integer n, int fallback) { return n == null ? fallback : n; }
    private static double decimal(BigDecimal n) { return n == null ? 0 : n.doubleValue(); }
    private static String date(LocalDate d) { return d == null ? "" : d.format(DATE_FMT); }
    private String componentDate(LocalDate d, EsignTemplateComponent c) {
        if (d == null) return "";
        String format = null;
        try {
            if (StringUtils.hasText(c.getSpecialAttribute()))
                format = objectMapper.readTree(c.getSpecialAttribute()).path("dateFormat").asText(null);
        } catch (Exception ignored) {}
        if (!StringUtils.hasText(format)) return date(d);
        try { return d.format(DateTimeFormatter.ofPattern(format)); }
        catch (IllegalArgumentException ignored) { return date(d); }
    }
    private void validateComponentValueLength(EsignTemplateComponent c, String value) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(c.getSpecialAttribute())) return;
        int maxLength;
        try {
            String raw = objectMapper.readTree(c.getSpecialAttribute()).path("componentMaxLength").asText();
            if (!StringUtils.hasText(raw) || "null".equals(raw)) return;
            maxLength = Integer.parseInt(raw);
        } catch (Exception ignored) { return; }
        int units = value.codePoints().map(codePoint -> codePoint <= 127 ? 1 : 2).sum();
        if (units > maxLength * 2) {
            throw BusinessException.badRequest("合同字段“" + c.getComponentName() + "”内容超过e签宝模板限制（最多"
                    + maxLength + "个汉字或" + (maxLength * 2) + "个英文字符），请调整模板控件长度");
        }
    }
    private static String yuan(BigDecimal fen) { return fen == null ? "0" : fen.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
    private static String leaseYears(Integer months) { return BigDecimal.valueOf(nvl(months, 1)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
}
