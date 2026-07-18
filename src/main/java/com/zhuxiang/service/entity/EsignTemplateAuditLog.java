package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("esign_template_audit_log")
public class EsignTemplateAuditLog {
    @TableId
    private String id;
    private String templateId;
    private String action;
    private String operatorId;
    private String operatorName;
    private String detailText;
    private LocalDateTime createdAt;
}
