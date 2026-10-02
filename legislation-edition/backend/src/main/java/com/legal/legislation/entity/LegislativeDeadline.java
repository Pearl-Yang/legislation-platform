package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 立法项目期限节点 - 模块一
 * 立项时根据 stage_template 自动生成；定时任务扫描 remind_before_days 触发提醒
 */
@Data
@TableName("legislative_deadline")
@Schema(description = "立法项目期限节点（用于到期提醒）")
public class LegislativeDeadline {

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_DONE     = "DONE";
    public static final String STATUS_OVERDUE  = "OVERDUE";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属立法项目 ID", example = "1")
    @TableField("project_id")
    private Long projectId;

    @Schema(description = "期限节点名称", example = "提交二审稿")
    @TableField("node_name")
    private String nodeName;

    @Schema(description = "截止日期", example = "2024-12-31")
    @TableField("deadline_date")
    private LocalDate deadlineDate;

    @Schema(description = "提前提醒天数", example = "7")
    @TableField("remind_before_days")
    private Integer remindBeforeDays;

    @Schema(description = "状态：PENDING 待办 / DONE 已完成 / OVERDUE 已逾期",
        example = "PENDING", allowableValues = {"PENDING", "DONE", "OVERDUE"})
    @TableField("status")
    private String status;

    @Schema(description = "最近一次提醒时间", example = "2024-12-24T09:00:00")
    @TableField("reminded_at")
    private LocalDateTime remindedAt;
}