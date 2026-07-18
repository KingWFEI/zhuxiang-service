package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminContractTemplateDtos;
import com.zhuxiang.service.service.AdminContractTemplateService;
import com.zhuxiang.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequireAuth
@RequestMapping("/admin/contract-templates")
public class AdminContractTemplateController {
    private final AdminContractTemplateService service;
    private final UserService userService;

    public AdminContractTemplateController(AdminContractTemplateService service, UserService userService) { this.service = service; this.userService = userService; }

    @GetMapping
    public ApiResponse<PageData<AdminContractTemplateDtos.Summary>> list(
            HttpServletRequest req,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        operator(req); return ApiResponse.success(service.list(page, Math.min(pageSize, 100), keyword, status));
    }

    @GetMapping("/{id}") public ApiResponse<AdminContractTemplateDtos.Detail> get(HttpServletRequest req, @PathVariable String id) { operator(req); return ApiResponse.success(service.get(id)); }
    @PostMapping public ApiResponse<AdminContractTemplateDtos.Detail> create(HttpServletRequest req, @Valid @RequestBody AdminContractTemplateDtos.CreateRequest body) { return ApiResponse.success("模板创建成功", service.create(body, operator(req))); }
    @PostMapping("/{id}/source-file") public ApiResponse<AdminContractTemplateDtos.Detail> upload(HttpServletRequest req, @PathVariable String id, @RequestParam("file") MultipartFile file) { return ApiResponse.success("源文件上传成功", service.uploadSource(id, file, operator(req))); }
    @PostMapping("/{id}/esign/create-url") public ApiResponse<AdminContractTemplateDtos.PageLink> createUrl(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success(service.createUrl(id, operator(req))); }
    @PostMapping("/{id}/esign/edit-url") public ApiResponse<AdminContractTemplateDtos.PageLink> editUrl(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success(service.editUrl(id, operator(req))); }
    @PostMapping("/{id}/sync-components") public ApiResponse<List<AdminContractTemplateDtos.ComponentView>> sync(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success("控件同步成功", service.sync(id, operator(req))); }
    @GetMapping("/{id}/components") public ApiResponse<List<AdminContractTemplateDtos.ComponentView>> components(HttpServletRequest req, @PathVariable String id) { operator(req); return ApiResponse.success(service.components(id)); }
    @PutMapping("/{id}/component-mappings") public ApiResponse<List<AdminContractTemplateDtos.ComponentView>> saveMappings(HttpServletRequest req, @PathVariable String id, @Valid @RequestBody AdminContractTemplateDtos.SaveMappingsRequest body) { return ApiResponse.success("映射保存成功", service.saveMappings(id, body, operator(req))); }
    @PostMapping("/{id}/validate") public ApiResponse<AdminContractTemplateDtos.ValidationResult> validate(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success(service.validate(id, operator(req))); }
    @PostMapping("/{id}/generate-test-file") public ApiResponse<AdminContractTemplateDtos.Preview> testFile(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success(service.generateTestFile(id, operator(req))); }
    @PostMapping("/{id}/publish") public ApiResponse<AdminContractTemplateDtos.Detail> publish(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success("模板发布成功", service.publish(id, operator(req))); }
    @PostMapping("/{id}/offline") public ApiResponse<AdminContractTemplateDtos.Detail> offline(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success("模板已下线", service.offline(id, operator(req))); }
    @PostMapping("/{id}/clone-version") public ApiResponse<AdminContractTemplateDtos.Detail> cloneVersion(HttpServletRequest req, @PathVariable String id) { return ApiResponse.success("新版本创建成功", service.cloneVersion(id, operator(req))); }
    @GetMapping("/{id}/preview-url") public ApiResponse<AdminContractTemplateDtos.Preview> preview(HttpServletRequest req, @PathVariable String id) { operator(req); return ApiResponse.success(service.preview(id)); }
    @GetMapping("/{id}/audit-logs") public ApiResponse<List<AdminContractTemplateDtos.AuditLog>> audit(HttpServletRequest req, @PathVariable String id) { operator(req); return ApiResponse.success(service.auditLogs(id)); }

    private String operator(HttpServletRequest req) {
        String id = CurrentUser.id(req);
        if (!"ADMIN".equals(userService.requireActiveUser(id).getRole())) throw BusinessException.forbidden("仅管理员可以配置合同模板");
        return id;
    }
}
