package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AppLegalDocumentDtos;
import com.zhuxiang.service.service.AppLegalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequireAuth
@RestController
@RequestMapping("/admin/app-management/legal-documents")
@Tag(name = "管理端 APP 管理", description = "管理 APP 用户协议和隐私政策")
@SecurityRequirement(name = "bearerAuth")
public class AdminAppManagementController {

    private final AppLegalDocumentService documentService;

    public AdminAppManagementController(AppLegalDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @Operation(summary = "查询 APP 协议内容")
    public ApiResponse<List<AppLegalDocumentDtos.DocumentView>> list(HttpServletRequest request) {
        return ApiResponse.success(documentService.listForAdmin(CurrentUser.id(request)));
    }

    @PutMapping("/{documentType}")
    @Operation(summary = "更新 APP 协议内容")
    public ApiResponse<AppLegalDocumentDtos.DocumentView> update(
            HttpServletRequest request,
            @PathVariable String documentType,
            @Valid @RequestBody AppLegalDocumentDtos.SaveDocumentRequest body
    ) {
        return ApiResponse.success("APP 协议内容已保存", documentService.updateForAdmin(
                documentType, body, CurrentUser.id(request)));
    }
}
