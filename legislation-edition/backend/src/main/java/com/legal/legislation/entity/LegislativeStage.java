package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 立法项目流程节点 - 模块一
 * 由 LegislativeFlowService 在立项时根据 legislative_stage_template 实例化生成
 */
@Data
@TableName("legislative_stage")
@Schema(description = "立法项目流程节点")
public class LegislativeStage {

    public static final String STATUS_PENDING     = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_DONE        = "DONE";
    public static final String STATUS_SKIPPED     = "SKIPPED";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属立法项目 ID", example = "1")
    @TableField("project_id")
    private Long projectId;

    @Schema(description = "阶段编码：DRAFT 起草 / REVIEW 审查 / CONSULTATION 征求意见 / PUBLISH 发布 / EVALUATION 评估", example = "DRAFT")
    @TableField("stage_code")
    private String stageCode;

    @Schema(description = "阶段名称", example = "起草阶段")
    @TableField("stage_name")
    private String stageName;

    @Schema(description = "阶段顺序号（从 1 开始）", example = "1")
    @TableField("stage_order")
    private Integer stageOrder;

    @Schema(description = "节点状态：PENDING 待办 / IN_PROGRESS 进行中 / DONE 已完成 / SKIPPED 已跳过",
        example = "IN_PROGRESS", allowableValues = {"PENDING", "IN_PROGRESS", "DONE", "SKIPPED"})
    @TableField("status")
    private String status;

    @Schema(description = "操作人 ID", example = "1")
    @TableField("operator_id")
    private Long operatorId;

    @Schema(description = "最近一次操作时间", example = "2024-01-01T12:00:00")
    @TableField("operator_time")
    private LocalDateTime operatorTime;

    @Schema(description = "备注", example = "完成征求意见汇总，进入下一阶段")
    @TableField("remark")
    private String remark;
}