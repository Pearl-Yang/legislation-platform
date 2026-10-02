package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 清理任务 - 模块四
 */
@Data
@TableName("cleanup_task")
@Schema(description = "清理任务")
public class CleanupTask {

    public static final String TYPE_DAILY      = "DAILY";      // 日常清理
    public static final String TYPE_PERIODIC   = "PERIODIC";   // 定期集中清理
    public static final String TYPE_THEMATIC   = "THEMATIC";   // 专项主题清理

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_DONE    = "DONE";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "任务名称", example = "2024Q1 规章集中清理")
    @TableField("task_name")
    private String taskName;

    @Schema(description = "任务类型：DAILY 日常 / PERIODIC 定期 / THEMATIC 专项",
        example = "PERIODIC", allowableValues = {"DAILY", "PERIODIC", "THEMATIC"})
    @TableField("task_type")
    private String taskType;

    @Schema(description = "状态：PENDING 待执行 / RUNNING 进行中 / DONE 已完成",
        example = "RUNNING", allowableValues = {"PENDING", "RUNNING", "DONE"})
    @TableField("status")
    private String status;

    @Schema(description = "触发本任务的上位法 ID（→ regulation.id，DAILY 类型为空）", example = "101")
    @TableField("trigger_regulation_id")
    private Long triggerRegulationId;

    @Schema(description = "创建人", example = "1")
    @TableField("created_by")
    private Long createdBy;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "完成时间", example = "2024-01-15T18:00:00")
    @TableField("completed_at")
    private LocalDateTime completedAt;

    @Schema(description = "清理模式：AUTO 自动 / MANUAL_REVIEW 人工复核 / HYBRIDHYBRID 混合", example = "HYBRID")
    @TableField("cleanup_mode")
    private String cleanupMode;

    @Schema(description = "专项主题关键词（仅 THEMATIC 类型有值）", example = "数据安全")
    @TableField("theme")
    private String theme;
}