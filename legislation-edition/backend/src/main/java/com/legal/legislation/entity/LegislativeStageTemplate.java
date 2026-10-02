package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 立法流程模板配置 - 模块一
 *
 * 把《行政法规制定程序条例》《规章制定程序条例》两个流程模板做成配置数据：
 * 当 LegislativeFlowService.initProjectStages() 时，根据 project.project_type 拉模板实例化。
 */
@Data
@TableName("legislative_stage_template")
@Schema(description = "立法流程模板配置（按类型：行政法规 / 部门规章 / 地方政府规章）")
public class LegislativeStageTemplate {

    public static final String TYPE_ADMIN_REGULATION = LegislativeProject.TYPE_ADMIN_REGULATION;
    public static final String TYPE_DEPT_RULE        = LegislativeProject.TYPE_DEPT_RULE;
    public static final String TYPE_LOCAL_RULE       = LegislativeProject.TYPE_LOCAL_RULE;

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "适用项目类型：ADMIN_REGULATION 行政法规 / DEPT_RULE 部门规章 / LOCAL_RULE 地方政府规章",
        example = "ADMIN_REGULATION")
    @TableField("type")
    private String type;

    @Schema(description = "阶段编码：DRAFT 起草 / REVIEW 审查 / CONSULTATION 征求意见 / PUBLISH 发布 / EVALUATION 评估",
        example = "DRAFT")
    @TableField("stage_code")
    private String stageCode;

    @Schema(description = "阶段名称", example = "起草阶段")
    @TableField("stage_name")
    private String stageName;

    @Schema(description = "阶段顺序号（从 1 开始）", example = "1")
    @TableField("stage_order")
    private Integer stageOrder;

    @Schema(description = "该阶段法定期限（天）", example = "30")
    @TableField("default_days")
    private Integer defaultDays;

    @Schema(description = "是否必经阶段：1 是 / 0 否", example = "1")
    @TableField("is_required")
    private Integer isRequired;
}