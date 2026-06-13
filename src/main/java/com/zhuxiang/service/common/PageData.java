package com.zhuxiang.service.common;

import java.util.List;

public record PageData<T>(
        List<T> items,
        long page,
        long pageSize,
        boolean hasMore,
        long total
) {
    public static <T> PageData<T> of(List<T> items, long page, long pageSize, long total) {
        return new PageData<>(items, page, pageSize, page * pageSize < total, total);
    }
}
