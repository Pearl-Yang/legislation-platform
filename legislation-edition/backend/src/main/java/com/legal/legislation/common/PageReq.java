package com.legal.legislation.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页请求参数。前端传 {page, size, keyword}，Service 层不再重复定义。
 */
@Data
@Schema(description = "分页请求")
public class PageReq {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private long page = 1;

    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private long size = 10;

    @Schema(description = "关键字（可选）")
    private String keyword;

    public long offset() {
        return Math.max(0, (page - 1) * size);
    }
}