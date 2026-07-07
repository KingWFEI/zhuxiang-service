package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.CustomerServiceDtos;
import com.zhuxiang.service.entity.CustomerServiceKbDocument;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档管理服务
 */
public interface KbDocumentService extends IService<CustomerServiceKbDocument> {

    /**
     * 上传并创建知识库文档。
     */
    CustomerServiceDtos.KbDocumentView uploadDocument(
            String operatorId, MultipartFile file, String title, String category
    );

    /**
     * 分页查询知识库文档列表。
     */
    PageData<CustomerServiceDtos.KbDocumentView> listDocuments(
            String operatorId, String status, String category, long page, long pageSize
    );

    /**
     * 更新文档元数据（标题、分类、启用/停用）。
     */
    CustomerServiceDtos.KbDocumentView updateDocument(
            String operatorId, String documentId, CustomerServiceDtos.UpdateKbDocumentRequest request
    );

    /**
     * 触发重新向量化。
     */
    void triggerVectorize(String operatorId, String documentId);

    /**
     * 删除文档及其向量数据。
     */
    void deleteDocument(String operatorId, String documentId);
}
