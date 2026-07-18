package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("esign_template_component")
public class EsignTemplateComponent {
    @TableId
    private String id;
    private String templateId;
    private String componentId;
    private String componentKey;
    private String componentName;
    private Integer componentType;
    private Integer requiredFlag;
    private Integer pageNum;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private BigDecimal componentWidth;
    private BigDecimal componentHeight;
    private String signerRole;
    private String specialAttribute;
    private String mappingMode;
    private String businessFieldCode;
    private String fixedValue;
    private Integer editableFlag;
    private String syncStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
