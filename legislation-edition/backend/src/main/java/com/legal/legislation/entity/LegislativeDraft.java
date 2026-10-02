package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 立法草案 - 模块二
 *
 * AI 生成后存为一条记录；每次生成产生新版本，
 * 修改后通过 draft_version_history 记录历史。
 */
@Data
@TableName("legislative_draft")
@Schema(description = "立法草案")
public class LegislativeDraft {

    public static final String GEN_TYPE_AUTO     = "AUTO_GENERATED";
    public static final String GEN_TYPE_MANUAL   = "MANUAL";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属立法项目 ID", example = "1")
    @TableField("project_id")
    private Long projectId;

    @Schema(description = "所属阶段 ID（→ legislative_stage.id）", example = "3")
    @TableField("stage_id")
    private Long stageId;

    @Schema(description = "草案正文", example = "第一章 总则\n第一条 ...")
    @TableField("draft_content")
    private String draftContent;

    @Schema(description = "参考的上位法 ID（→ regulation.id）", example = "101")
    @TableField("source_regulation_id")
    private Long sourceRegulationId;

    @Schema(description = "引用的异地规章片段，JSON 数组 [{regulationId, excerpt}]",
        example = "[{\"regulationId\":103,\"excerpt\":\"...\"}]")
    @TableField("referenced_texts")
    private String referencedTexts;

    @Schema(description = "当前版本号", example = "3")
    @TableField("version")
    private Integer version;

    @Schema(description = "生成方式：AUTO_GENERATED 自动生成 / MANUAL 人工起草",
        example = "AUTO_GENERATED", allowableValues = {"AUTO_GENERATED", "MANUAL"})
    @TableField("generation_type")
    private String generationType;

    @Schema(description = "生成时的提示词快照，便于回溯")
    @TableField("prompt_snapshot")
    private String promptSnapshot;

    @Schema(description = "创建人", example = "1")
    @TableField("created_by")
    private Long createdBy;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间", example = "2024-01-01T12:00:00")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}