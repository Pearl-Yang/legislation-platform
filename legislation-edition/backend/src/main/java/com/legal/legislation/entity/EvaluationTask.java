package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评估任务 - 模块五
 */
@Data
@TableName("evaluation_task")
@Schema(description = "评估任务")
public class EvaluationTask {

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_RUNNING  = "RUNNING";
    public static final String STATUS_COMPLETE = "COMPLETED";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "被评估的法规 ID", example = "1")
    @TableField("regulation_id")
    private Long regulationId;

    @Schema(description = "评估周期起始日", example = "2024-01-01")
    @TableField("period_start")
    private java.time.LocalDate periodStart;

    @Schema(description = "评估周期结束日", example = "2024-12-31")
    @TableField("period_end")
    private java.time.LocalDate periodEnd;

    @Schema(description = "综合得分（0~100）", example = "85.50")
    @TableField("overall_score")
    private java.math.BigDecimal overallScore;

    @Schema(description = "任务状态：PENDING 待执行 / RUNNING 进行中 / COMPLETED 已完成",
        example = "RUNNING", allowableValues = {"PENDING", "RUNNING", "COMPLETED"})
    @TableField("status")
    private String status;

    @Schema(description = "评估报告内容（HTML / Markdown）")
    @TableField("report_content")
    private String reportContent;

    @Schema(description = "任务创建人", example = "1")
    @TableField("created_by")
    private Long createdBy;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "完成时间", example = "2024-01-15T18:00:00")
    @TableField("completed_at")
    private LocalDateTime completedAt;
}