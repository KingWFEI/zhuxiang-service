package com.zhuxiang.service.immersive.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile upload(MultipartFile file, String directory);
    void delete(String fileUrl);
    boolean isLocalUrl(String fileUrl);
}
