package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资料-标签关联（中间表） - 模块七
 */
@Data
@TableName("material_tag")
@Schema(description = "资料-标签关联（中间表）")
public class MaterialTag {

    @Schema(description = "资料 ID（→ library_material.id）", example = "1")
    @TableField("material_id")
    private Long materialId;

    @Schema(description = "标签 ID（→ library_tag.id）", example = "1")
    @TableField("tag_id")
    private Long tagId;
}