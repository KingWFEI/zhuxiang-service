package com.zhuxiang.service.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页数据")
public record PageData<T>(
        @Schema(description = "当前页数据") List<T> items,
        @Schema(description = "当前页码，从 1 开始", example = "1") long page,
        @Schema(description = "每页条数", example = "20") long pageSize,
        @Schema(description = "是否还有下一页", example = "true") boolean hasMore,
        @Schema(description = "总记录数", example = "42") long total
) {
    public static <T> PageData<T> of(List<T> items, long page, long pageSize, long total) {
        return new PageData<>(items, page, pageSize, page * pageSize < total, total);
    }
}
