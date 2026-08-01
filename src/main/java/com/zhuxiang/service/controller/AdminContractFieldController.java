package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminContractTemplateDtos;
import com.zhuxiang.service.service.AdminContractTemplateService;
import com.zhuxiang.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireAuth
@RequestMapping("/admin/contract-field-definitions")
@Tag(name = "管理端合同字段", description = "查询合同模板可映射的系统业务字段目录")
@SecurityRequirement(name = "bearerAuth")
public class AdminContractFieldController {
    private final AdminContractTemplateService service;
    private final UserService userService;
    public AdminContractFieldController(AdminContractTemplateService service, UserService userService) { this.service = service; this.userService = userService; }
    @GetMapping
    @Operation(summary = "查询合同字段目录", description = "返回合同控件可配置的系统字段、派生字段及其支持的控件类型。")
    public ApiResponse<List<AdminContractTemplateDtos.FieldDefinition>> list(HttpServletRequest request) {
        if (!"ADMIN".equals(userService.requireActiveUser(CurrentUser.id(request)).getRole())) throw BusinessException.forbidden("仅管理员可以配置合同模板");
        return ApiResponse.success(service.fieldDefinitions());
    }
}
