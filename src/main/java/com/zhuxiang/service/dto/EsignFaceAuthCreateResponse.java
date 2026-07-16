package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

/**
 * e签宝个人人脸认证发起响应。
 * <p>
 * 只映射必要字段：code / message / data.flowId / data.authUrl / data.expire。
 * originalUrl 和 faceToken 不予映射。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EsignFaceAuthCreateResponse {

    private int code;
    private String message;
    private CreateFaceAuthData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateFaceAuthData {
        private String flowId;

        /** 临时 H5 认证地址。禁止完整日志输出 */
        @ToString.Exclude
        private String authUrl;

        /** 过期时间戳（毫秒） */
        private Long expire;
    }
}
