package com.zhuxiang.service.common;

/**
 * e签宝 V3 合同接口异常。
 */
public class EsignException extends RuntimeException {

    private final int httpStatus;
    private final String esignCode;
    private final String path;

    public EsignException(int httpStatus, String esignCode, String message, String path) {
        super(message);
        this.httpStatus = httpStatus;
        this.esignCode = esignCode;
        this.path = path;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getEsignCode() { return esignCode; }
    public String getPath() { return path; }

    public static EsignException signingFailed(String esignCode, String message, String path) {
        return new EsignException(200, esignCode, message, path);
    }

    public static EsignException configError(String message) {
        return new EsignException(0, "CONFIG_ERROR", message, null);
    }

    public static EsignException notSigned() {
        return new EsignException(400, "NOT_COMPLETED", "合同尚未完成签署，不能下载", null);
    }

    /** 对外的业务错误说明 */
    public String toUserMessage() {
        String msg = getMessage();
        if ("1437328".equals(esignCode)) {
            return "签署手机号对应的实名信息与当前用户实名信息不一致";
        }
        if ("INVALID_SIGNATURE".equals(esignCode)) {
            return "e签宝请求签名失败，请检查系统配置";
        }
        if (msg != null && msg.contains("控件")) {
            return "电子合同模板配置已变化，请联系管理员";
        }
        if (msg != null && (msg.contains("超出") || msg.contains("区域"))) {
            return "合同字段内容过长，请检查房屋地址或其他字段";
        }
        return msg != null ? msg : "e签宝服务异常";
    }
}
