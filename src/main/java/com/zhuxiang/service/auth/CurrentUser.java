package com.zhuxiang.service.auth;

import com.zhuxiang.service.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

public final class CurrentUser {

    public static final String USER_ID_ATTRIBUTE = CurrentUser.class.getName() + ".userId";
    public static final String USER_ROLE_ATTRIBUTE = CurrentUser.class.getName() + ".userRole";

    private CurrentUser() {
    }

    public static String id(HttpServletRequest request) {
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或 Token 失效");
        }
        return userId.toString();
    }

    public static String optionalId(HttpServletRequest request) {
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);
        return userId == null ? null : userId.toString();
    }

    public static String role(HttpServletRequest request) {
        Object role = request.getAttribute(USER_ROLE_ATTRIBUTE);
        if (role == null) {
            throw BusinessException.unauthorized("未登录或 Token 失效");
        }
        return role.toString();
    }
}
