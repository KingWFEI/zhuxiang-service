package com.zhuxiang.service.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        if (isSseRequest(request) && (exception.getCode() == 401 || exception.getCode() == 403)) {
            log.debug("SSE鉴权未通过: {} {} -> code={}",
                    request.getMethod(), request.getRequestURI(), exception.getCode());
        } else {
            log.warn("接口请求失败: {} {} -> code={}, message={}",
                    request.getMethod(), request.getRequestURI(), exception.getCode(), exception.getMessage());
        }
        if (isSseRequest(request) && (exception.getCode() == 401 || exception.getCode() == 403)) {
            return ResponseEntity.status(exception.getCode()).build();
        }
        return ResponseEntity.status(exception.getCode())
                .body(ApiResponse.error(
                        exception.getCode(), exception.getMessage(), exception.getData()
                ));
    }

    private boolean isSseRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return request.getRequestURI().endsWith("/messages/stream")
                || (accept != null && accept.contains("text/event-stream"));
    }

    @ExceptionHandler(EsignApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleEsignApiException(
            EsignApiException exception,
            HttpServletRequest request
    ) {
        log.warn("e签宝接口调用失败: {} {} -> httpStatus={}, esignCode={}, path={}, message={}",
                request.getMethod(), request.getRequestURI(),
                exception.getHttpStatus(), exception.getEsignCode(),
                exception.getPath(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, "实名认证服务暂时不可用，请稍后重试"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "请求参数错误" : fieldError.getDefaultMessage();
        log.warn("接口参数错误: {} {} -> message={}", request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception, HttpServletRequest request) {
        log.warn("接口参数错误: {} {} -> message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadSize(HttpServletRequest request) {
        String message = "上传文件不能超过 5MB";
        log.warn("接口请求失败: {} {} -> code=400, message={}",
                request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(EsignException.class)
    public ResponseEntity<ApiResponse<Void>> handleEsignException(
            EsignException exception,
            HttpServletRequest request
    ) {
        log.warn("e签宝V3合同接口失败: {} {} -> code={}, message={}, path={}",
                request.getMethod(), request.getRequestURI(),
                exception.getEsignCode(), exception.getMessage(), exception.getPath());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, exception.toUserMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnknownException(Exception exception, HttpServletRequest request) {
        if (isSseRequest(request) && isClientDisconnect(exception)) {
            log.debug("SSE客户端已断开: {} {}", request.getMethod(), request.getRequestURI());
            return ResponseEntity.noContent().build();
        }
        log.error("接口未知异常: {} {} -> message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误"));
    }

    private boolean isClientDisconnect(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String typeName = current.getClass().getName();
            if (typeName.contains("AsyncRequestNotUsableException")
                    || typeName.contains("ClientAbortException")) {
                return true;
            }
            if (current instanceof IOException) {
                String message = current.getMessage();
                return message == null
                        || message.contains("中止了一个已建立的连接")
                        || message.contains("Connection reset")
                        || message.contains("Broken pipe");
            }
            current = current.getCause();
        }
        return false;
    }
}
