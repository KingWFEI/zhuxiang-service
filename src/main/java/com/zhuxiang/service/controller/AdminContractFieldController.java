package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminContractTemplateDtos;
import com.zhuxiang.service.service.AdminContractTemplateService;
import com.zhuxiang.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequireAuth
@RequestMapping("/admin/contract-field-definitions")
public class AdminContractFieldController {
    private final AdminContractTemplateService service;
    private final UserService userService;
    public AdminContractFieldController(AdminContractTemplateService service, UserService userService) { this.service = service; this.userService = userService; }
    @GetMapping public ApiResponse<List<AdminContractTemplateDtos.FieldDefinition>> list(HttpServletRequest request) {
        if (!"ADMIN".equals(userService.requireActiveUser(CurrentUser.id(request)).getRole())) throw BusinessException.forbidden("仅管理员可以配置合同模板");
        return ApiResponse.success(service.fieldDefinitions());
    }
}
