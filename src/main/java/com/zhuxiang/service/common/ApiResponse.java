package com.zhuxiang.service.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一接口响应")
public record ApiResponse<T>(
        @Schema(description = "业务状态码，与 HTTP 状态码保持一致", example = "200") int code,
        @Schema(description = "处理结果说明", example = "success") String message,
        @Schema(description = "响应数据；无返回数据时为 null") T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
