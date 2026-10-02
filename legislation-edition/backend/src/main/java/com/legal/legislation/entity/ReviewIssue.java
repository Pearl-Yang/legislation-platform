package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审查问题 - 模块三
 * severity 字段对应"红 / 黄 / 蓝 / 灰"四级提示
 */
@Data
@TableName("review_issue")
@Schema(description = "审查问题（红/黄/蓝/灰四级）")
public class ReviewIssue {

    public static final String SEVERITY_RED    = "RED";     // 与上位法冲突 / 越权 - 必须修改
    public static final String SEVERITY_YELLOW = "YELLOW";  // 引用失效 / 重复 - 建议修改
    public static final String SEVERITY_BLUE   = "BLUE";    // 格式不规范 - 提示
    public static final String SEVERITY_GREY   = "GREY";    // 语言冗杂 - AI 建议

    public static final String TYPE_SUPERIOR_CONFLICT = "SUPERIOR_CONFLICT";
    public static final String TYPE_OVER_POWER        = "OVER_POWER";
    public static final String TYPE_OUTDATED_REF      = "OUTDATED_REF";
    public static final String TYPE_DUPLICATE         = "DUPLICATE";
    public static final String TYPE_FORMAT            = "FORMAT";
    public static final String TYPE_VERBOSE           = "VERBOSE";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属审查记录 ID", example = "100")
    @TableField("review_record_id")
    private Long reviewRecordId;

    @Schema(description = "问题类型：SUPERIOR_CONFLICT 上位法冲突 / OVER_POWER 越权 / OUTDATED_REF 引用失效 / DUPLICATE 重复 / FORMAT 格式 / VERBOSE 冗余",
        example = "SUPERIOR_CONFLICT")
    @TableField("issue_type")
    private String issueType;

    @Schema(description = "严重级别（红：RED / 黄：YELLOW / 蓝：BLUE / 灰：GREY）",
        example = "RED", allowableValues = {"RED", "YELLOW", "BLUE", "GREY"})
    @TableField("severity")
    private String severity;

    @Schema(description = "问题所在条款编号", example = "第二十条")
    @TableField("article_index")
    private String articleIndex;

    @Schema(description = "问题描述", example = "本条与《数据安全法》第二十一条存在冲突")
    @TableField("description")
    private String description;

    @Schema(description = "修改建议", example = "建议调整表述为 ...")
    @TableField("suggestion")
    private String suggestion;

    @Schema(description = "关联参考规章 ID（→ regulation.id）", example = "101")
    @TableField("reference_regulation_id")
    private Long referenceRegulationId;

    @Schema(description = "是否已解决：1 已解决 / 0 未解决", example = "0")
    @TableField("is_resolved")
    private Integer isResolved;
}