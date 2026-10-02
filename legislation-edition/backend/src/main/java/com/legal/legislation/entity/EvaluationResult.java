package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 评估结果明细 - 模块五
 */
@Data
@TableName("evaluation_result")
@Schema(description = "评估结果明细（每个评估任务 × 指标 一行）")
public class EvaluationResult {

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属评估任务 ID", example = "1")
    @TableField("task_id")
    private Long taskId;

    @Schema(description = "指标 ID（→ evaluation_indicator.id）", example = "1")
    @TableField("indicator_id")
    private Long indicatorId;

    @Schema(description = "原始数据值（未经归一化）", example = "78")
    @TableField("raw_value")
    private BigDecimal rawValue;

    @Schema(description = "归一化后得分（0~100）", example = "78.00")
    @TableField("normalized_score")
    private BigDecimal normalizedScore;

    @Schema(description = "当期权重（可与指标 weight 不同）", example = "0.30")
    @TableField("weight")
    private BigDecimal weight;

    @Schema(description = "所属维度的小计分", example = "85.20")
    @TableField("dimension_score")
    private BigDecimal dimensionScore;

    @Schema(description = "在同类法规中的排名（可为 null）", example = "3 / 50")
    @TableField("rank")
    private String rank;

    @Schema(description = "原始数据快照，JSON")
    @TableField("data_snapshot")
    private String dataSnapshot;

    @Schema(description = "评估周期标签", example = "2024Q1")
    @TableField("period_label")
    private String periodLabel;
}