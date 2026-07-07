package com.zhuxiang.service.immersive.storage;

public class StoredFile {
    private final String url;
    private final String originalFilename;
    private final long size;

    public StoredFile(String url, String originalFilename, long size) {
        this.url = url;
        this.originalFilename = originalFilename;
        this.size = size;
    }

    public String getUrl() { return url; }
    public String getOriginalFilename() { return originalFilename; }
    public long getSize() { return size; }
}
