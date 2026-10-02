package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审查规则 - 模块三
 *
 * check_logic 用 JSON 表达式配置规则条件，规则引擎求值。
 */
@Data
@TableName("review_rule")
@Schema(description = "审查规则（规则引擎配置）")
public class ReviewRule {

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "规则编码（系统内唯一）", example = "R001_SUPERIOR_CONFLICT")
    @TableField("rule_code")
    private String ruleCode;

    @Schema(description = "规则名称", example = "上位法冲突检测")
    @TableField("rule_name")
    private String ruleName;

    @Schema(description = "规则类型：LEGAL 合规 / CONSISTENCY 一致性 / RISK 风险 / LANGUAGE 语言",
        example = "LEGAL")
    @TableField("rule_type")
    private String ruleType;

    @Schema(description = "规则条件 JSON 表达式，由规则引擎求值")
    @TableField("check_logic")
    private String checkLogic;

    @Schema(description = "严重级别（RED/YELLOW/BLUE/GREY）", example = "RED")
    @TableField("severity")
    private String severity;

    @Schema(description = "是否启用：1 启用 / 0 禁用", example = "1")
    @TableField("enabled")
    private Integer enabled;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间", example = "2024-01-01T12:00:00")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}