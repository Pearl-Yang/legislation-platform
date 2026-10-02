package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 立法资料批注 - 模块七
 */
@Data
@TableName("material_note")
@Schema(description = "立法资料批注")
public class MaterialNote {

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "被批注的资料 ID", example = "1")
    @TableField("material_id")
    private Long materialId;

    @Schema(description = "批注人 ID", example = "1")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "批注内容")
    @TableField("note_content")
    private String noteContent;

    @Schema(description = "高亮的原文片段")
    @TableField("highlighted_text")
    private String highlightedText;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    @TableField("created_at")
    private LocalDateTime createdAt;
}