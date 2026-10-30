package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("app_legal_document")
public class AppLegalDocument implements Serializable {
    @TableId("document_type")
    private String documentType;
    private String title;
    private String contentMarkdown;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
