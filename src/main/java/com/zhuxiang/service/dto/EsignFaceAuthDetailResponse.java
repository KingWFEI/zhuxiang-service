package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

/**
 * e签宝个人人脸认证详情响应。
 * <p>
 * 不映射：facePhotoUrl、facePhotoAllUrl、faceVideoUrl、idCardFrontPicUrl、
 * idCardBackPicUrl、faceVisionResult、livingScore、similarity 等敏感字段。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EsignFaceAuthDetailResponse {

    private int code;
    private String message;
    private FaceAuthDetailData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FaceAuthDetailData {

        private String flowId;
        private String status;
        private String objectType;
        private Long startTime;
        private Long endTime;
        private String authType;
        private String failReason;
        private IndivInfo indivInfo;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IndivInfo {
            private String accountId;
            private String name;

            /** 证件号。禁止日志和 toString 输出 */
            @ToString.Exclude
            private String certNo;

            private String certType;
            private String mobileNo;
        }
    }
}
