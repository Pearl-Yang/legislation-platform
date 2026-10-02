package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 清理建议 - 模块四
 */
@Data
@TableName("cleanup_suggestion")
@Schema(description = "清理建议（AI + 人工处置结论）")
public class CleanupSuggestion {

    public static final String SUG_KEEP      = "KEEP";      // 保留
    public static final String SUG_MODIFY    = "MODIFY";    // 修改
    public static final String SUG_OBSOLETE  = "OBSOLETE";  // 废止

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属清理任务 ID", example = "1")
    @TableField("task_id")
    private Long taskId;

    @Schema(description = "被建议处置的法规 ID（→ regulation.id）", example = "101")
    @TableField("regulation_id")
    private Long regulationId;

    @Schema(description = "AI 建议：KEEP 保留 / MODIFY 修改 / OBSOLETE 废止",
        example = "MODIFY", allowableValues = {"KEEP", "MODIFY", "OBSOLETE"})
    @TableField("suggestion")
    private String suggestion;

    @Schema(description = "AI 给出建议的理由", example = "已被新法替代，且与上位法存在冲突")
    @TableField("reason")
    private String reason;

    @Schema(description = "AI 给出建议的置信度（0~1）", example = "0.87")
    @TableField("ai_confidence")
    private BigDecimal aiConfidence;

    @Schema(description = "最终人工决策：KEEP 保留 / MODIFY 修改 / OBSOLETE 废止", example = "OBSOLETE")
    @TableField("final_decision")
    private String finalDecision;

    @Schema(description = "最终决策人 ID", example = "1")
    @TableField("decided_by")
    private Long decidedBy;

    @Schema(description = "最终决策时间", example = "2024-01-15T18:00:00")
    @TableField("decided_at")
    private LocalDateTime decidedAt;
}