package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AppLegalDocumentDtos;
import com.zhuxiang.service.entity.AppLegalDocument;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.AppLegalDocumentMapper;
import com.zhuxiang.service.service.AppLegalDocumentService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AppLegalDocumentServiceImpl
        extends ServiceImpl<AppLegalDocumentMapper, AppLegalDocument>
        implements AppLegalDocumentService {

    private static final Set<String> DOCUMENT_TYPES = Set.of("USER_AGREEMENT", "PRIVACY_POLICY");
    private static final List<String> DOCUMENT_ORDER = List.of("USER_AGREEMENT", "PRIVACY_POLICY");

    private final UserService userService;

    public AppLegalDocumentServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public List<AppLegalDocumentDtos.DocumentView> listForAdmin(String operatorId) {
        requireAdmin(operatorId);
        return list(Wrappers.<AppLegalDocument>lambdaQuery()
                        .orderByAsc(AppLegalDocument::getDocumentType))
                .stream()
                .sorted((left, right) -> Integer.compare(
                        DOCUMENT_ORDER.indexOf(left.getDocumentType()),
                        DOCUMENT_ORDER.indexOf(right.getDocumentType())))
                .map(this::toView)
                .toList();
    }

    @Override
    public AppLegalDocumentDtos.DocumentView getForApp(String documentType) {
        AppLegalDocument document = getById(normalizeDocumentType(documentType));
        if (document == null) {
            throw BusinessException.notFound("协议内容不存在");
        }
        return toView(document);
    }

    @Override
    @Transactional
    public AppLegalDocumentDtos.DocumentView updateForAdmin(
            String documentType,
            AppLegalDocumentDtos.SaveDocumentRequest request,
            String operatorId
    ) {
        requireAdmin(operatorId);
        String normalizedType = normalizeDocumentType(documentType);
        AppLegalDocument document = getById(normalizedType);
        if (document == null) {
            throw BusinessException.notFound("协议内容不存在");
        }
        document.setContentMarkdown(request.contentMarkdown());
        document.setUpdatedBy(operatorId);
        document.setUpdatedAt(LocalDateTime.now());
        if (!updateById(document)) {
            throw new IllegalStateException("协议内容保存失败");
        }
        return toView(document);
    }

    private String normalizeDocumentType(String documentType) {
        String normalized = documentType == null
                ? ""
                : documentType.trim().toUpperCase(Locale.ROOT);
        if (!DOCUMENT_TYPES.contains(normalized)) {
            throw BusinessException.notFound("协议类型不存在");
        }
        return normalized;
    }

    private void requireAdmin(String operatorId) {
        User operator = userService.requireActiveUser(operatorId);
        if (!"ADMIN".equalsIgnoreCase(operator.getRole())) {
            throw BusinessException.forbidden("当前账号无权管理 APP 协议内容");
        }
    }

    private AppLegalDocumentDtos.DocumentView toView(AppLegalDocument document) {
        return new AppLegalDocumentDtos.DocumentView(
                document.getDocumentType(),
                document.getTitle(),
                document.getContentMarkdown(),
                document.getUpdatedAt()
        );
    }
}
