package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 立法草案版本历史 - 模块二
 */
@Data
@TableName("draft_version_history")
@Schema(description = "立法草案版本历史")
public class DraftVersionHistory {

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属草案 ID", example = "1001")
    @TableField("draft_id")
    private Long draftId;

    @Schema(description = "版本号", example = "2")
    @TableField("version")
    private Integer version;

    @Schema(description = "该版本的内容快照")
    @TableField("content_snapshot")
    private String contentSnapshot;

    @Schema(description = "变更人 ID", example = "1")
    @TableField("changed_by")
    private Long changedBy;

    @Schema(description = "变更时间", example = "2024-01-01T12:00:00")
    @TableField("changed_at")
    private LocalDateTime changedAt;

    @Schema(description = "变更摘要", example = "调整第二章法律责任")
    @TableField("change_summary")
    private String changeSummary;
}