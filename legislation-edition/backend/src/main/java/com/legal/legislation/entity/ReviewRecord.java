package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审查记录 - 模块三
 */
@Data
@TableName("review_record")
@Schema(description = "审查记录")
public class ReviewRecord {

    public static final String REVIEW_TYPE_AUTO   = "AUTO";
    public static final String REVIEW_TYPE_MANUAL = "MANUAL";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE    = "DONE";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "被审查的草案 ID", example = "1001")
    @TableField("draft_id")
    private Long draftId;

    @Schema(description = "审查方式：AUTO 自动审查 / MANUAL 人工审查",
        example = "AUTO", allowableValues = {"AUTO", "MANUAL"})
    @TableField("review_type")
    private String reviewType;

    @Schema(description = "状态：PENDING 待执行 / DONE 已完成",
        example = "DONE", allowableValues = {"PENDING", "DONE"})
    @TableField("status")
    private String status;

    @Schema(description = "审查人 ID（人工审查时填）", example = "1")
    @TableField("reviewed_by")
    private Long reviewedBy;

    @Schema(description = "审查完成时间", example = "2024-01-01T12:00:00")
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @Schema(description = "整体是否通过：1 通过 / 0 未通过", example = "1")
    @TableField("overall_pass")
    private Integer overallPass;

    @Schema(description = "错误（红色）级别问题数量", example = "2")
    @TableField("error_count")
    private Integer errorCount;
}