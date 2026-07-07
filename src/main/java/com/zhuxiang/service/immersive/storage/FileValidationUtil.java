package com.zhuxiang.service.immersive.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

/**
 * 文件校验工具：通过魔数校验文件类型。
 */
public final class FileValidationUtil {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png");
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private FileValidationUtil() {}

    public static void validate(String filename, String contentType, byte[] content) {
        if (filename == null || filename.isBlank()) throw new IllegalArgumentException("文件名为空");
        int dot = filename.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException("无法识别文件类型（缺少扩展名）");
        String ext = filename.substring(dot + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("不支持的文件格式: " + ext);
        if (contentType != null && !ALLOWED_MIME.contains(contentType))
            throw new IllegalArgumentException("不支持的MIME类型: " + contentType);
        if (content.length < 3) throw new IllegalArgumentException("文件内容过短");
        if (!matchesMagic(content, JPEG_MAGIC) && !matchesMagic(content, PNG_MAGIC))
            throw new IllegalArgumentException("文件内容不是有效图片");
    }

    private static boolean matchesMagic(byte[] content, byte[] magic) {
        if (content.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++)
            if (content[i] != magic[i]) return false;
        return true;
    }
}
