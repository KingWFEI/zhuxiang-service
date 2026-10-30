package com.zhuxiang.service.controller;

import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AppLegalDocumentDtos;
import com.zhuxiang.service.service.AppLegalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/legal-documents")
@Tag(name = "APP 协议内容", description = "公开查询用户协议和隐私政策")
public class AppLegalDocumentController {

    private final AppLegalDocumentService documentService;

    public AppLegalDocumentController(AppLegalDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{documentType}")
    @Operation(summary = "查询 APP 协议内容")
    public ApiResponse<AppLegalDocumentDtos.DocumentView> get(@PathVariable String documentType) {
        return ApiResponse.success(documentService.getForApp(documentType));
    }
}
