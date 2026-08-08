package com.zhuxiang.service.common;

public class BusinessException extends RuntimeException {

    private final int code;
    private final Object data;

    public BusinessException(int code, String message) {
        this(code, message, null);
    }

    public BusinessException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }

    public static BusinessException tooManyRequests(String message) {
        return new BusinessException(429, message);
    }

    public static BusinessException tooManyRequests(String message, Object data) {
        return new BusinessException(429, message, data);
    }

    public static BusinessException serviceUnavailable(String message) {
        return new BusinessException(503, message);
    }

    /** 用户未完成全局实名认证，不可创建租约订单 */
    public static BusinessException realNameRequired() {
        return new BusinessException(400, "请先完成实名认证");
    }
}
