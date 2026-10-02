package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 立法资料库条目 - 模块七
 *
 * material_type 决定存储内容类型：
 *  REGULATION / DRAFT / REPORT / EXPERT_OPINION / CASE
 */
@Data
@TableName("library_material")
@Schema(description = "立法资料库条目")
public class LibraryMaterial {

    public static final String TYPE_REGULATION      = "REGULATION";
    public static final String TYPE_DRAFT          = "DRAFT";
    public static final String TYPE_REPORT         = "REPORT";
    public static final String TYPE_EXPERT_OPINION  = "EXPERT_OPINION";
    public static final String TYPE_CASE           = "CASE";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "资料标题", example = "《数据安全法》立法说明会会议纪要")
    @TableField("title")
    private String title;

    @Schema(description = "资料类型：REGULATION 法规 / DRAFT 草案 / REPORT 评估报告 / EXPERT_OPINION 专家意见 / CASE 典型案例",
        example = "REGULATION", allowableValues = {"REGULATION", "DRAFT", "REPORT", "EXPERT_OPINION", "CASE"})
    @TableField("material_type")
    private String materialType;

    @Schema(description = "行政区划代码", example = "110000")
    @TableField("region_code")
    private String regionCode;

    @Schema(description = "发布机构", example = "司法部")
    @TableField("issuing_authority")
    private String issuingAuthority;

    @Schema(description = "发布日期", example = "2024-01-01")
    @TableField("issue_date")
    private LocalDate issueDate;

    @Schema(description = "生效日期（仅 REGULATION 类型有值）", example = "2024-09-01")
    @TableField("effective_date")
    private LocalDate effectiveDate;

    @Schema(description = "关键词")
    @TableField("keywords")
    private String keywords;

    @Schema(description = "原始文件 URL")
    @TableField("file_url")
    private String fileUrl;

    @Schema(description = "摘要")
    @TableField("digest")
    private String digest;

    @Schema(description = "全文内容")
    @TableField("full_text")
    private String fullText;

    @Schema(description = "被引用次数", example = "12")
    @TableField("reference_count")
    private Integer referenceCount;

    @Schema(description = "浏览次数", example = "385")
    @TableField("view_count")
    private Integer viewCount;

    @Schema(description = "录入人 ID", example = "1")
    @TableField("created_by")
    private Long createdBy;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间", example = "2024-01-01T12:00:00")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}