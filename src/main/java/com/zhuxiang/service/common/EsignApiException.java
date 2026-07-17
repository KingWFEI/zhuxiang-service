package com.zhuxiang.service.common;

/**
 * e签宝 API 调用异常。
 * <p>
 * 不包含 AppSecret、完整姓名、完整身份证号、rawBody、authUrl 等敏感信息。
 */
public class EsignApiException extends RuntimeException {

    private final int httpStatus;
    private final int esignCode;
    private final String path;

    public EsignApiException(int httpStatus, int esignCode, String message, String path) {
        super(message);
        this.httpStatus = httpStatus;
        this.esignCode = esignCode;
        this.path = path;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int getEsignCode() {
        return esignCode;
    }

    public String getPath() {
        return path;
    }
}
