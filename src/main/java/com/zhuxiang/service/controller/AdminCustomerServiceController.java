package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceSession;
import com.zhuxiang.service.service.CustomerServiceMessageService;
import com.zhuxiang.service.service.CustomerServiceSessionService;
import com.zhuxiang.service.service.KbDocumentService;
import com.zhuxiang.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * 管理端智能客服接口
 */
@Validated
@RequireAuth
@RestController
@RequestMapping("/admin/customer-service")
@Tag(name = "管理端智能客服", description = "管理端知识库文档管理和客服会话查询")
@SecurityRequirement(name = "bearerAuth")
public class AdminCustomerServiceController {

    private static final Logger log = LoggerFactory.getLogger(AdminCustomerServiceController.class);

    private final KbDocumentService kbDocumentService;
    private final CustomerServiceSessionService sessionService;
    private final CustomerServiceMessageService messageService;
    private final UserService userService;

    public AdminCustomerServiceController(
            KbDocumentService kbDocumentService,
            CustomerServiceSessionService sessionService,
            CustomerServiceMessageService messageService,
            UserService userService
    ) {
        this.kbDocumentService = kbDocumentService;
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.userService = userService;
    }

    // ========== 知识库管理 ==========

    /**
     * 上传知识库文档。
     */
    @PostMapping("/kb/documents")
    @Operation(summary = "上传知识库文档", description = "上传 PDF、DOCX、TXT 或 MD 文档，最大 20MB")
    public ApiResponse<CustomerServiceDtos.KbDocumentView> uploadDocument(
            HttpServletRequest request,
            @Parameter(description = "文档文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "文档标题，不传则使用文件名") @RequestParam(required = false) String title,
            @Parameter(description = "文档分类") @RequestParam(required = false) String category
    ) {
        return ApiResponse.success(kbDocumentService.uploadDocument(
                CurrentUser.id(request), file, title, category
        ));
    }

    /**
     * 查询知识库文档列表。
     */
    @GetMapping("/kb/documents")
    @Operation(summary = "知识库文档列表", description = "分页查询知识库文档，可按状态和分类筛选")
    public ApiResponse<PageData<CustomerServiceDtos.KbDocumentView>> listDocuments(
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "分类筛选") @RequestParam(required = false) String category,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") @Min(1) @Max(50) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(kbDocumentService.listDocuments(
                CurrentUser.id(request), status, category, page, pageSize
        ));
    }

    /**
     * 更新知识库文档元数据。
     */
    @PutMapping("/kb/documents/{documentId}")
    @Operation(summary = "更新知识库文档", description = "更新文档标题、分类，或启用/停用文档")
    public ApiResponse<CustomerServiceDtos.KbDocumentView> updateDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @RequestBody @Valid CustomerServiceDtos.UpdateKbDocumentRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(kbDocumentService.updateDocument(
                CurrentUser.id(request), documentId, body
        ));
    }

    /**
     * 重新向量化。
     */
    @PostMapping("/kb/documents/{documentId}/vectorize")
    @Operation(summary = "重新向量化", description = "对指定文档触发重新向量化处理")
    public ApiResponse<Void> vectorize(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            HttpServletRequest request
    ) {
        log.info("管理端请求向量化: documentId={}", documentId);
        kbDocumentService.triggerVectorize(CurrentUser.id(request), documentId);
        return ApiResponse.success(null);
    }

    /**
     * 删除知识库文档。
     */
    @DeleteMapping("/kb/documents/{documentId}")
    @Operation(summary = "删除知识库文档", description = "软删除知识库文档，同时通知Agent删除向量数据")
    public ApiResponse<Void> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            HttpServletRequest request
    ) {
        kbDocumentService.deleteDocument(CurrentUser.id(request), documentId);
        return ApiResponse.success(null);
    }

    // ========== 会话管理（管理端查看） ==========

    /**
     * 管理端查看所有客服会话列表。
     */
    @GetMapping("/sessions")
    @Operation(summary = "客服会话列表", description = "管理端查看所有用户的客服会话")
    public ApiResponse<PageData<CustomerServiceDtos.SessionItem>> listSessions(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") @Min(1) @Max(50) long pageSize,
            HttpServletRequest request
    ) {
        // 校验管理权限
        userService.requireActiveUser(CurrentUser.id(request));
        IPage<CustomerServiceSession> result = ((com.baomidou.mybatisplus.extension.service.IService<CustomerServiceSession>) sessionService)
                .page(new Page<>(page, pageSize),
                        Wrappers.<CustomerServiceSession>lambdaQuery()
                                .isNull(CustomerServiceSession::getDeletedAt)
                                .orderByDesc(CustomerServiceSession::getUpdatedAt));

        List<CustomerServiceDtos.SessionItem> items = result.getRecords().stream()
                .map(s -> new CustomerServiceDtos.SessionItem(
                        s.getId(), s.getTitle(), s.getStatus(), s.getMessageCount(),
                        s.getLastMessagePreview(), s.getClosedReason(), s.getLastMessageAt(),
                        s.getCreatedAt(), s.getUpdatedAt()))
                .toList();
        return ApiResponse.success(PageData.of(items, page, pageSize, result.getTotal()));
    }

    /**
     * 管理端查看会话消息。
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "查看会话消息", description = "管理端查看指定会话的消息记录")
    public ApiResponse<List<CustomerServiceDtos.MessageItem>> getMessages(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        userService.requireActiveUser(CurrentUser.id(request));
        return ApiResponse.success(messageService.getMessagesBySessionId(sessionId));
    }
}
