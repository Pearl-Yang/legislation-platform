package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 立法项目主表 - 模块一
 * 对应需求：行政立法项目全流程管理模块
 *
 * 一条记录 = 一个立法项目（行政法规 / 部门规章 / 地方政府规章）
 */
@Data
@TableName("legislative_project")
@Schema(description = "立法项目主表")
public class LegislativeProject {

    public static final String TYPE_ADMIN_REGULATION   = "ADMIN_REGULATION";    // 行政法规
    public static final String TYPE_DEPT_RULE          = "DEPT_RULE";           // 部门规章
    public static final String TYPE_LOCAL_RULE         = "LOCAL_RULE";          // 地方政府规章

    public static final String STATUS_DRAFT    = "DRAFT";     // 草稿
    public static final String STATUS_ACTIVE   = "ACTIVE";    // 进行中
    public static final String STATUS_PUBLISHED = "PUBLISHED"; // 已发布
    public static final String STATUS_OBSOLETE = "OBSOLETE";  // 已废止

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "立法项目名称", example = "网络数据安全管理条例")
    @TableField("project_name")
    private String projectName;

    @Schema(description = "项目类型：ADMIN_REGULATION 行政法规 / DEPT_RULE 部门规章 / LOCAL_RULE 地方政府规章",
        example = "ADMIN_REGULATION", allowableValues = {"ADMIN_REGULATION", "DEPT_RULE", "LOCAL_RULE"})
    @TableField("project_type")
    private String projectType;

    @Schema(description = "项目说明", example = "为规范网络数据处理活动，保障数据安全与合法权益")
    @TableField("description")
    private String description;

    @Schema(description = "项目状态：DRAFT 草稿 / ACTIVE 进行中 / PUBLISHED 已发布 / OBSOLETE 已废止",
        example = "ACTIVE", allowableValues = {"DRAFT", "ACTIVE", "PUBLISHED", "OBSOLETE"})
    @TableField("status")
    private String status;

    @Schema(description = "发布日期", example = "2024-06-15")
    @TableField("publish_date")
    private LocalDate publishDate;

    @Schema(description = "创建人用户 ID", example = "1")
    @TableField("created_by")
    private Long createdBy;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间", example = "2024-01-01T12:00:00")
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除标记：0 未删 / 1 已删", example = "0", hidden = true)
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}