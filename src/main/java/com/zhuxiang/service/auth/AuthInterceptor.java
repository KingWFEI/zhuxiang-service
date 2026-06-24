package com.zhuxiang.service.auth;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenProvider tokenProvider;
    private final UserService userService;

    public AuthInterceptor(TokenProvider tokenProvider, UserService userService) {
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        boolean required = handlerMethod.hasMethodAnnotation(RequireAuth.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAuth.class);
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            if (required) {
                throw BusinessException.unauthorized("未登录或 Token 失效");
            }
            return true;
        }
        String userId = tokenProvider.parseAccessToken(authorization.substring(7).trim());
        userService.requireActiveUser(userId);
        request.setAttribute(CurrentUser.USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
