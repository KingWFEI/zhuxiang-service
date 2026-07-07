package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档表
 */
@TableName(value = "customer_service_kb_document")
@Data
public class CustomerServiceKbDocument implements Serializable {

    @TableId
    private String id;

    /** 文档标题 */
    private String title;

    /** 文档分类 */
    private String category;

    /** 原始文件名 */
    private String originalFilename;

    /** 文件类型：PDF DOCX TXT MD */
    private String fileType;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 存储路径 */
    private String filePath;

    /** 向量分块数量 */
    private Integer chunkCount;

    /** 状态：PENDING PROCESSING ACTIVE DISABLED FAILED */
    private String status;

    /** 处理失败原因 */
    private String errorMessage;

    /** 向量化失败重试次数 */
    private Integer vectorizeFailedCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 软删除时间 */
    private LocalDateTime deletedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
