package com.zhuxiang.service.immersive.common;

import com.zhuxiang.service.common.BusinessException;

/**
 * 沉浸式模块业务异常工厂。
 * 映射原 ErrorCode 到 zhuxiang-service 的 BusinessException 静态工厂方法。
 */
public final class ImmersiveErrors {

    private ImmersiveErrors() {}

    public static BusinessException tourNotFound() {
        return BusinessException.notFound("项目不存在");
    }

    public static BusinessException houseAlreadyHasTour() {
        return BusinessException.conflict("该房源已创建沉浸式项目");
    }

    public static BusinessException tourStatusNotAllowed(String detail) {
        return BusinessException.conflict(detail != null ? detail : "项目状态不允许此操作");
    }

    public static BusinessException sceneNotFound() {
        return BusinessException.notFound("房间不存在");
    }

    public static BusinessException imageNotFound() {
        return BusinessException.notFound("图片不存在");
    }

    public static BusinessException hotspotNotFound() {
        return BusinessException.notFound("热点不存在");
    }

    public static BusinessException entrySceneNotSet() {
        return BusinessException.badRequest("入口房间未设置");
    }

    public static BusinessException publishValidationFailed(String detail) {
        return BusinessException.badRequest(detail != null ? detail : "发布校验失败");
    }

    public static BusinessException tourNotPublished() {
        return BusinessException.badRequest("项目未发布");
    }

    public static BusinessException houseNotFound() {
        return BusinessException.notFound("房源不存在");
    }

    public static BusinessException featureDisabled() {
        return BusinessException.conflict("沉浸式看房功能已关闭");
    }

    public static BusinessException badRequest(String msg) {
        return BusinessException.badRequest(msg);
    }

    public static BusinessException notFound(String msg) {
        return BusinessException.notFound(msg);
    }

    public static BusinessException conflict(String msg) {
        return BusinessException.conflict(msg);
    }
}
