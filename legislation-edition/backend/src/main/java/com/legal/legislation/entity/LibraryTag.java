package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 立法资料标签 - 模块七
 */
@Data
@TableName("library_tag")
@Schema(description = "立法资料标签")
public class LibraryTag {

    public static final String TYPE_DOMAIN  = "DOMAIN";  // 领域
    public static final String TYPE_LEVEL   = "LEVEL";   // 层级
    public static final String TYPE_REGION  = "REGION";  // 地区

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "标签名称", example = "数据安全")
    @TableField("tag_name")
    private String tagName;

    @Schema(description = "标签类型：DOMAIN 领域 / LEVEL 层级 / REGION 地区",
        example = "DOMAIN", allowableValues = {"DOMAIN", "LEVEL", "REGION"})
    @TableField("tag_type")
    private String tagType;

    @Schema(description = "被使用的次数", example = "32")
    @TableField("usage_count")
    private Integer usageCount;
}