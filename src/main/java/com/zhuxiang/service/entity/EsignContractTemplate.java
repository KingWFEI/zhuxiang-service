package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("esign_contract_template")
public class EsignContractTemplate {
    @TableId
    private String id;
    private String businessType;
    private String templateCode;
    private String templateName;
    private Integer version;
    private String environment;
    private String docTemplateId;
    private String sourceFileId;
    private String sourceFileName;
    private Integer templateType;
    private String status;
    private String componentFingerprint;
    private Long esignCreateTime;
    private Long esignUpdateTime;
    private LocalDateTime lastSyncedAt;
    private String validationStatus;
    private String validationMessage;
    private String versionNote;
    private String createdBy;
    private String publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer versionLock;
}
