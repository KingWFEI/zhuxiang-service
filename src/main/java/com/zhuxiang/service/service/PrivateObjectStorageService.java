package com.zhuxiang.service.service;

import java.io.InputStream;

/**
 * 私有对象存储。对象不得通过静态目录公开，只能由鉴权接口读取。
 */
public interface PrivateObjectStorageService {

    /**
     * 保存私有对象，返回可持久化的对象键。
     */
    String storePrivate(String objectKey, InputStream input, long size, String contentType);

    /**
     * 打开私有对象流。
     */
    StoredPrivateObject openPrivate(String objectKey);

    record StoredPrivateObject(
            InputStream inputStream,
            long contentLength,
            String contentType
    ) {
    }
}

