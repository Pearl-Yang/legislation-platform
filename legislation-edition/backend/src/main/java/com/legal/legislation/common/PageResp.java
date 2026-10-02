package com.legal.legislation.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应。前端拿到 records/total 直接渲染表格。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应")
public class PageResp<T> {

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "当前页码")
    private long page;

    @Schema(description = "每页条数")
    private long size;

    public static <T> PageResp<T> empty(long page, long size) {
        return new PageResp<>(Collections.emptyList(), 0, page, size);
    }
}