package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 法规上下位 / 引用关系 - 模块四
 *
 * 同时写 MySQL（用于合规 SQL 检索）和 Neo4j（用于图谱可视化）。
 */
@Data
@TableName("regulation_relation")
@Schema(description = "法规关系（同时写入 Neo4j）")
public class RegulationRelation {

    public static final String TYPE_SUPERIOR     = "SUPERIOR";       // 上位 → 下位
    public static final String TYPE_REFERENCE    = "REFERENCE";      // 引用
    public static final String TYPE_SUBSTITUTE   = "SUBSTITUTE";     // 替代
    public static final String TYPE_OBSOLETE    = "OBSOLETE";       // 废止

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "源法规 ID（→ regulation.id）", example = "101")
    @TableField("source_id")
    private Long sourceId;

    @Schema(description = "目标法规 ID（→ regulation.id）", example = "202")
    @TableField("target_id")
    private Long targetId;

    @Schema(description = "关系类型：SUPERIOR 上下位 / REFERENCE 引用 / SUBSTITUTE 替代 / OBSOLETE 废止",
        example = "SUPERIOR", allowableValues = {"SUPERIOR", "REFERENCE", "SUBSTITUTE", "OBSOLETE"})
    @TableField("relation_type")
    private String relationType;

    @Schema(description = "关联的具体条款", example = "第二十条")
    @TableField("related_article")
    private String relatedArticle;

    @Schema(description = "关系描述", example = "上位法依据")
    @TableField("description")
    private String description;

    @Schema(description = "AI 抽取置信度（0~1）", example = "0.95")
    @TableField("confidence_score")
    private BigDecimal confidenceScore;
}