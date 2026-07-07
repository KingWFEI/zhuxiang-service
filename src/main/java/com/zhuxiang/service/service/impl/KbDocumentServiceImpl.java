package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.client.CustomerServiceAgentClient;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceEnums;
import com.zhuxiang.service.entity.CustomerServiceKbDocument;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.CustomerServiceKbDocumentMapper;
import com.zhuxiang.service.service.KbDocumentService;
import com.zhuxiang.service.service.ObjectStorageService;
import com.zhuxiang.service.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 知识库文档管理服务实现
 */
@Service
public class KbDocumentServiceImpl
        extends ServiceImpl<CustomerServiceKbDocumentMapper, CustomerServiceKbDocument>
        implements KbDocumentService {

    private static final Logger log = LoggerFactory.getLogger(KbDocumentServiceImpl.class);

    /** 允许的文档类型及对应扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".txt", ".md");

    /** 文件大小限制：20MB */
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

    /** Admin角色 */
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "HOUSEKEEPER");

    private static final DateTimeFormatter PATH_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final UserService userService;
    private final ObjectStorageService objectStorageService;
    private final CustomerServiceAgentClient agentClient;

    @Value("${app.upload.directory}")
    private String uploadDirectory;

    public KbDocumentServiceImpl(
            UserService userService,
            ObjectStorageService objectStorageService,
            CustomerServiceAgentClient agentClient
    ) {
        this.userService = userService;
        this.objectStorageService = objectStorageService;
        this.agentClient = agentClient;
    }

    @Override
    @Transactional
    public CustomerServiceDtos.KbDocumentView uploadDocument(
            String operatorId, MultipartFile file, String title, String category
    ) {
        requireAdminRole(operatorId);
        validateFile(file);

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw BusinessException.badRequest("仅支持 PDF、DOCX、TXT、MD 格式");
        }

        // 保存文件
        String objectKey = "kb-documents/" + LocalDate.now().format(PATH_DATE)
                + "/" + UUID.randomUUID() + extension;
        String filePath;
        try (InputStream input = file.getInputStream()) {
            filePath = objectStorageService.store(
                    objectKey, input, file.getSize(), file.getContentType()
            );
        } catch (IOException e) {
            throw new IllegalStateException("文件保存失败", e);
        }

        // 创建文档记录
        String docTitle = StringUtils.hasText(title) ? title : file.getOriginalFilename();
        CustomerServiceKbDocument doc = new CustomerServiceKbDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setTitle(docTitle);
        doc.setCategory(StringUtils.hasText(category) ? category : CustomerServiceEnums.KbDocumentCategory.GENERAL);
        doc.setOriginalFilename(file.getOriginalFilename());
        doc.setFileType(extension.replace(".", "").toUpperCase());
        doc.setFileSize(file.getSize());
        doc.setFilePath(filePath);
        doc.setChunkCount(0);
        doc.setStatus(CustomerServiceEnums.KbDocumentStatus.PENDING);
        doc.setVectorizeFailedCount(0);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        save(doc);

        // 异步触发向量化（失败不影响上传）
        triggerVectorizeAsync(doc);

        return toView(doc);
    }

    @Override
    public PageData<CustomerServiceDtos.KbDocumentView> listDocuments(
            String operatorId, String status, String category, long page, long pageSize
    ) {
        requireAdminRole(operatorId);
        IPage<CustomerServiceKbDocument> result = page(
                new Page<>(page, pageSize),
                Wrappers.<CustomerServiceKbDocument>lambdaQuery()
                        .isNull(CustomerServiceKbDocument::getDeletedAt)
                        .eq(StringUtils.hasText(status), CustomerServiceKbDocument::getStatus, status)
                        .eq(StringUtils.hasText(category), CustomerServiceKbDocument::getCategory, category)
                        .orderByDesc(CustomerServiceKbDocument::getCreatedAt)
        );
        return PageData.of(
                result.getRecords().stream().map(this::toView).toList(),
                page,
                pageSize,
                result.getTotal()
        );
    }

    @Override
    @Transactional
    public CustomerServiceDtos.KbDocumentView updateDocument(
            String operatorId, String documentId, CustomerServiceDtos.UpdateKbDocumentRequest request
    ) {
        requireAdminRole(operatorId);
        CustomerServiceKbDocument doc = getById(documentId);
        if (doc == null || doc.getDeletedAt() != null) {
            throw BusinessException.notFound("文档不存在");
        }

        if (request.title() != null) {
            doc.setTitle(request.title());
        }
        if (request.category() != null) {
            doc.setCategory(request.category());
        }
        if (request.status() != null) {
            String newStatus = request.status();
            if (!CustomerServiceEnums.KbDocumentStatus.ACTIVE.equals(newStatus)
                    && !CustomerServiceEnums.KbDocumentStatus.DISABLED.equals(newStatus)) {
                throw BusinessException.badRequest("仅可将状态设置为 ACTIVE 或 DISABLED");
            }
            doc.setStatus(newStatus);
        }
        doc.setUpdatedAt(LocalDateTime.now());
        updateById(doc);
        return toView(doc);
    }

    @Override
    @Transactional
    public void triggerVectorize(String operatorId, String documentId) {
        requireAdminRole(operatorId);
        CustomerServiceKbDocument doc = getById(documentId);
        if (doc == null || doc.getDeletedAt() != null) {
            throw BusinessException.notFound("文档不存在");
        }
        // 重置状态并触发向量化（lambdaUpdate 确保 null 字段也被写入）
        update(Wrappers.<CustomerServiceKbDocument>lambdaUpdate()
                .eq(CustomerServiceKbDocument::getId, doc.getId())
                .set(CustomerServiceKbDocument::getStatus, CustomerServiceEnums.KbDocumentStatus.PROCESSING)
                .set(CustomerServiceKbDocument::getErrorMessage, null)
                .set(CustomerServiceKbDocument::getUpdatedAt, LocalDateTime.now())
        );
        triggerVectorizeAsync(doc);
    }

    @Override
    @Transactional
    public void deleteDocument(String operatorId, String documentId) {
        requireAdminRole(operatorId);
        CustomerServiceKbDocument doc = getById(documentId);
        if (doc == null || doc.getDeletedAt() != null) {
            throw BusinessException.notFound("文档不存在");
        }
        // 通知 Agent 删除向量
        agentClient.deleteVectors(documentId);
        // 软删除
        doc.setDeletedAt(LocalDateTime.now());
        doc.setStatus(CustomerServiceEnums.KbDocumentStatus.DISABLED);
        doc.setUpdatedAt(LocalDateTime.now());
        updateById(doc);
    }

    /** 异步触发向量化 */
    private void triggerVectorizeAsync(CustomerServiceKbDocument doc) {
        update(Wrappers.<CustomerServiceKbDocument>lambdaUpdate()
                .eq(CustomerServiceKbDocument::getId, doc.getId())
                .set(CustomerServiceKbDocument::getStatus, CustomerServiceEnums.KbDocumentStatus.PROCESSING)
                .set(CustomerServiceKbDocument::getUpdatedAt, LocalDateTime.now())
        );

        // 将存储 URL 转换为本地绝对路径，Python Agent 需要本地文件
        String localPath = resolveLocalPath(doc.getFilePath());

        int chunkCount = agentClient.triggerVectorize(
                doc.getId(), localPath, doc.getTitle(), doc.getCategory()
        );
        if (chunkCount > 0) {
            update(Wrappers.<CustomerServiceKbDocument>lambdaUpdate()
                    .eq(CustomerServiceKbDocument::getId, doc.getId())
                    .set(CustomerServiceKbDocument::getStatus, CustomerServiceEnums.KbDocumentStatus.ACTIVE)
                    .set(CustomerServiceKbDocument::getChunkCount, chunkCount)
                    .set(CustomerServiceKbDocument::getErrorMessage, null)
                    .set(CustomerServiceKbDocument::getVectorizeFailedCount, 0)
                    .set(CustomerServiceKbDocument::getUpdatedAt, LocalDateTime.now())
            );
        } else {
            update(Wrappers.<CustomerServiceKbDocument>lambdaUpdate()
                    .eq(CustomerServiceKbDocument::getId, doc.getId())
                    .set(CustomerServiceKbDocument::getStatus, CustomerServiceEnums.KbDocumentStatus.FAILED)
                    .set(CustomerServiceKbDocument::getErrorMessage, "Agent 服务调用失败，可稍后重新向量化")
                    .set(CustomerServiceKbDocument::getVectorizeFailedCount, doc.getVectorizeFailedCount() + 1)
                    .set(CustomerServiceKbDocument::getUpdatedAt, LocalDateTime.now())
            );
        }
    }

    /** 将存储 URL 转为本地绝对路径 */
    private String resolveLocalPath(String filePath) {
        // URL 格式: /api/uploads/kb-documents/2026/07/07/uuid.docx
        // 对应本地: <uploadDirectory>/kb-documents/2026/07/07/uuid.docx
        String prefix = "/api/uploads/";
        if (filePath != null && filePath.startsWith(prefix)) {
            return Path.of(uploadDirectory).resolve(filePath.substring(prefix.length()))
                    .toAbsolutePath().normalize().toString();
        }
        // 如果是 http/https URL（COS），下载到临时目录
        if (filePath != null && (filePath.startsWith("http://") || filePath.startsWith("https://"))) {
            try {
                java.io.File tmpDir = new java.io.File(uploadDirectory, "kb-tmp");
                tmpDir.mkdirs();
                String filename = filePath.substring(filePath.lastIndexOf('/') + 1);
                java.io.File tmpFile = new java.io.File(tmpDir, filename);
                try (java.io.InputStream is = java.net.URI.create(filePath).toURL().openStream();
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpFile)) {
                    is.transferTo(fos);
                }
                return tmpFile.getAbsolutePath();
            } catch (Exception e) {
                log.error("下载 COS 文件失败: {}", filePath, e);
            }
        }
        return filePath;
    }

    /** 校验操作人是否为管理员或管家 */
    private void requireAdminRole(String operatorId) {
        User operator = userService.requireActiveUser(operatorId);
        if (operator.getRole() == null || !ADMIN_ROLES.contains(operator.getRole())) {
            throw BusinessException.forbidden("仅管理员或管家可操作知识库");
        }
    }

    /** 校验文件 */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("文件大小不能超过 20MB");
        }
    }

    /** 从文件名提取扩展名（小写） */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }

    private CustomerServiceDtos.KbDocumentView toView(CustomerServiceKbDocument doc) {
        return new CustomerServiceDtos.KbDocumentView(
                doc.getId(),
                doc.getTitle(),
                doc.getCategory(),
                doc.getOriginalFilename(),
                doc.getFileType(),
                doc.getFileSize(),
                doc.getFilePath(),
                doc.getChunkCount(),
                doc.getStatus(),
                doc.getErrorMessage(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
