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

    /** 删除对象存储中的对象，用于数据库事务回滚后的补偿清理。 */
    void delete(String objectKey);
}
