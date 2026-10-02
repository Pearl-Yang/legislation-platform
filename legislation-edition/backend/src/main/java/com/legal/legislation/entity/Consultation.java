package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 意见征集公告 - 模块六
 */
@Data
@TableName("consultation")
@Schema(description = "意见征集公告")
public class Consultation {

    public static final String STATUS_DRAFT  = "DRAFT";
    public static final String STATUS_OPEN   = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "征集标题", example = "关于《网络数据安全管理条例（草案）》公开征求意见")
    @TableField("title")
    private String title;

    @Schema(description = "征集说明")
    @TableField("description")
    private String description;

    @Schema(description = "关联立法项目 ID（可空）", example = "1")
    @TableField("related_project_id")
    private Long relatedProjectId;

    @Schema(description = "关联草案 ID（可空）", example = "1001")
    @TableField("related_draft_id")
    private Long relatedDraftId;

    @Schema(description = "关联清理任务 ID（可空）", example = "1")
    @TableField("related_cleanup_task_id")
    private Long relatedCleanupTaskId;

    @Schema(description = "征集状态：DRAFT 草稿 / OPEN 征集中 / CLOSED 已结束",
        example = "OPEN", allowableValues = {"DRAFT", "OPEN", "CLOSED"})
    @TableField("status")
    private String status;

    @Schema(description = "征集开始日期", example = "2024-01-01")
    @TableField("start_date")
    private LocalDate startDate;

    @Schema(description = "征集截止日期", example = "2024-02-01")
    @TableField("end_date")
    private LocalDate endDate;

    @Schema(description = "累计浏览量", example = "1532")
    @TableField("total_views")
    private Integer totalViews;

    @Schema(description = "累计意见数", example = "248")
    @TableField("total_opinions")
    private Integer totalOpinions;

    @Schema(description = "公告创建人", example = "1")
    @TableField("created_by")
    private Long createdBy;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    @TableField("created_at")
    private LocalDateTime createdAt;
}