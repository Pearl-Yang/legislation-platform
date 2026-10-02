package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 意见分类（每期征集自动生成若干类别） - 模块六
 */
@Data
@TableName("opinion_category")
@Schema(description = "意见分类（自动分类结果 + 情感倾向汇总）")
public class OpinionCategory {

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属征集 ID", example = "1")
    @TableField("consultation_id")
    private Long consultationId;

    @Schema(description = "类别名称", example = "数据安全")
    @TableField("category_name")
    private String categoryName;

    @Schema(description = "本类别下的意见总数", example = "120")
    @TableField("opinion_count")
    private Integer opinionCount;

    @Schema(description = "支持性意见数", example = "65")
    @TableField("support_count")
    private Integer supportCount;

    @Schema(description = "反对意见数", example = "20")
    @TableField("oppose_count")
    private Integer opposeCount;

    @Schema(description = "中性 / 建议性意见数", example = "35")
    @TableField("neutral_count")
    private Integer neutralCount;
}