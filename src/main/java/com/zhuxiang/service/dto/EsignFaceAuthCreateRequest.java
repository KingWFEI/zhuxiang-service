package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

/**
 * e签宝个人人脸认证发起请求。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsignFaceAuthCreateRequest {

    /** 姓名 */
    private String name;

    /** 证件类型，默认 INDIVIDUAL_CH_IDCARD */
    private String certType = "INDIVIDUAL_CH_IDCARD";

    /** 证件号。禁止日志和 toString 输出 */
    @ToString.Exclude
    private String idNo;

    /** 认证模式，由配置注入 */
    private String faceauthMode;

    /** 人脸接口类型，固定 H5 */
    private String faceInterfaceType = "H5";

    /** 结果页策略，1 = 显示结果页 */
    private String resultPage = "1";

    /** 认证完成回调地址 */
    private String callbackUrl;

    /** 业务方上下文标识，由上层传入 */
    private String contextId;
}
