package com.zhuxiang.service.service;

import java.io.InputStream;

/**
 * 文件对象存储抽象。
 */
public interface ObjectStorageService {

    /**
     * 保存对象并返回前端可持久化使用的访问 URL。
     */
    String store(String objectKey, InputStream input, long size, String contentType);
}
