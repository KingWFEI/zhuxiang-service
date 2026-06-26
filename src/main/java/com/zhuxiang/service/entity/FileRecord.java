package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("file_record")
@Data
public class FileRecord implements Serializable {

    private String id;

    private String userId;

    private String url;

    private String bizType;

    private LocalDateTime createdAt;

    private static final long serialVersionUID = 1L;
}
