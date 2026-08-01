package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminContractTemplateDtos;
import com.zhuxiang.service.service.AdminContractTemplateService;
import com.zhuxiang.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequireAuth
@RequestMapping("/admin/contract-templates")
@Tag(name = "管理端合同模板", description = "合同底稿上传、e签宝模板制作、控件映射、校验、测试和发布全流程")
@SecurityRequirement(name = "bearerAuth")
public class AdminContractTemplateController {
    private final AdminContractTemplateService service;
    private final UserService userService;

    public AdminContractTemplateController(AdminContractTemplateService service, UserService userService) { this.service = service; this.userService = userService; }

    @GetMapping
    @Operation(summary = "分页查询合同模板", description = "按关键字和模板状态分页查询合同模板及其映射完成情况。")
    public ApiResponse<PageData<AdminContractTemplateDtos.Summary>> list(
            HttpServletRequest req,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        operator(req); return ApiResponse.success(service.list(page, Math.min(pageSize, 100), keyword, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询合同模板详情", description = "查询模板基础信息、e签宝模板信息、同步状态和校验结果。")
    public ApiResponse<AdminContractTemplateDtos.Detail> get(HttpServletRequest req, @PathVariable String id) { operator(req); return ApiResponse.success(service.get(id)); }
    @PostMapping
    @Operation(summary = "创建合同模板", description = "创建合同模板配置草稿，后续需要上传 PDF 底稿并完成控件映射。")
    public ApiResponse<AdminContractTemplateDtos.Detail> create(HttpServletRequest req, @Valid @RequestBody AdminContractTemplateDtos.CreateRequest body) { return ApiResponse.success("模板创建成功", service.create(body, operator(req))); }
    @PostMapping("/{id}/source-file")
    @Operation(summary = "上传合同 PDF 底稿", description = "上传合同 PDF 文件并保存为当前模板的源文件。")
    public ApiResponse<AdminContractTemplateDtos.Detail> upload(HttpServletRequest req, @PathVariable String id, @RequestParam("file") MultipartFile file) { return ApiResponse.success("源文件上传成功", service.uploadSource(id, file, operator(req))); }
    @PostMapping("/{id}/esign/create-url")
    @Operation(summary = "获取 e签宝模板创建地址", description = "将源文件提交至 e签宝并返回模板制作页面地址。")
    public ApiResponse<AdminContractTemplateDtos.PageLink> createUrl(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success(service.createUrl(id, operator(req))); }
    @PostMapping("/{id}/esign/edit-url")
    @Operation(summary = "获取 e签宝模板编辑地址", description = "获取当前 e签宝合同模板的在线编辑页面地址。")
    public ApiResponse<AdminContractTemplateDtos.PageLink> editUrl(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success(service.editUrl(id, operator(req))); }
    @PostMapping("/{id}/sync-components")
    @Operation(summary = "同步模板控件", description = "从 e签宝读取最新控件、componentKey、位置、尺寸和签署角色并保存到数据库。")
    public ApiResponse<List<AdminContractTemplateDtos.ComponentView>> sync(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success("控件同步成功", service.sync(id, operator(req))); }
    @GetMapping("/{id}/components")
    @Operation(summary = "查询模板控件", description = "查询当前模板已同步的全部控件及业务字段映射。")
    public ApiResponse<List<AdminContractTemplateDtos.ComponentView>> components(HttpServletRequest req, @PathVariable String id) { operator(req); return ApiResponse.success(service.components(id)); }
    @PutMapping("/{id}/component-mappings")
    @Operation(summary = "保存控件映射", description = "批量保存控件的映射模式、业务字段、固定值和签署角色配置。")
    public ApiResponse<List<AdminContractTemplateDtos.ComponentView>> saveMappings(HttpServletRequest req, @PathVariable String id, @Valid @RequestBody AdminContractTemplateDtos.SaveMappingsRequest body) { return ApiResponse.success("映射保存成功", service.saveMappings(id, body, operator(req))); }
    @PostMapping("/{id}/validate")
    @Operation(summary = "校验合同模板", description = "校验 componentKey 唯一性、必填字段映射、控件类型以及甲乙方签章位置。")
    public ApiResponse<AdminContractTemplateDtos.ValidationResult> validate(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success(service.validate(id, operator(req))); }
    @PostMapping("/{id}/generate-test-file")
    @Operation(summary = "生成测试合同", description = "使用测试业务数据填充模板并生成预览文件，用于发布前检查。")
    public ApiResponse<AdminContractTemplateDtos.Preview> testFile(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success(service.generateTestFile(id, operator(req))); }
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布合同模板", description = "发布校验通过的模板，并将同业务类型的旧活动版本下线。")
    public ApiResponse<AdminContractTemplateDtos.Detail> publish(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success("模板发布成功", service.publish(id, operator(req))); }
    @PostMapping("/{id}/offline")
    @Operation(summary = "下线合同模板", description = "停止使用当前模板处理新的合同签署。")
    public ApiResponse<AdminContractTemplateDtos.Detail> offline(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success("模板已下线", service.offline(id, operator(req))); }
    @PostMapping("/{id}/clone-version")
    @Operation(summary = "克隆合同模板版本", description = "复制当前模板及其控件映射，创建一个可继续编辑的新版本。")
    public ApiResponse<AdminContractTemplateDtos.Detail> cloneVersion(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success("新版本创建成功", service.cloneVersion(id, operator(req))); }
    @GetMapping("/{id}/preview-url")
    @Operation(summary = "获取模板预览地址", description = "获取合同模板源文件或测试合同的临时预览地址。")
    public ApiResponse<AdminContractTemplateDtos.Preview> preview(HttpServletRequest req, @PathVariable String id) { operator(req); return ApiResponse.success(service.preview(id)); }
    @GetMapping("/{id}/audit-logs")
    @Operation(summary = "查询模板审计日志", description = "查询模板创建、同步、校验、发布、漂移检测和下线记录。")
    public ApiResponse<List<AdminContractTemplateDtos.AuditLog>> audit(HttpServletRequest req, @PathVariable String id) { operator(req); return ApiResponse.success(service.auditLogs(id)); }

    private String operator(HttpServletRequest req) {
        String id = CurrentUser.id(req);
        if (!"ADMIN".equals(userService.requireActiveUser(id).getRole())) throw BusinessException.forbidden("仅管理员可以配置合同模板");
        return id;
    }
}
