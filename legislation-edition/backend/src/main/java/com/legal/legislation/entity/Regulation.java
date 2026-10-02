package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 法规主表 - 模块四 / 模块七 / 模块三
 *
 * 集中存储行政法规、部门规章、地方政府规章全文。
 * 是 cleanup（清理）、review（审查）、library（资料库）共用的核心数据。
 */
@Data
@TableName("regulation")
@Schema(description = "法规主表（行政法规 / 部门规章 / 地方政府规章全文）")
public class Regulation {

    public static final String TYPE_ADMIN_REGULATION = "ADMIN_REGULATION";
    public static final String TYPE_DEPT_RULE        = "DEPT_RULE";
    public static final String TYPE_LOCAL_RULE       = "LOCAL_RULE";

    public static final String STATUS_EFFECTIVE = "EFFECTIVE";  // 现行有效
    public static final String STATUS_REVISING  = "REVISING";   // 修订中
    public static final String STATUS_OBSOLETE  = "OBSOLETE";   // 已废止

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "法规名称", example = "网络数据安全管理条例")
    @TableField("regulation_name")
    private String regulationName;

    @Schema(description = "法规类型：ADMIN_REGULATION 行政法规 / DEPT_RULE 部门规章 / LOCAL_RULE 地方政府规章",
        example = "ADMIN_REGULATION", allowableValues = {"ADMIN_REGULATION", "DEPT_RULE", "LOCAL_RULE"})
    @TableField("regulation_type")
    private String regulationType;

    @Schema(description = "发布机关", example = "国务院")
    @TableField("issuing_authority")
    private String issuingAuthority;

    @Schema(description = "发文字号", example = "国务院令第 765 号")
    @TableField("issue_number")
    private String issueNumber;

    @Schema(description = "公布日期", example = "2024-06-15")
    @TableField("issue_date")
    private LocalDate issueDate;

    @Schema(description = "生效日期", example = "2024-09-01")
    @TableField("effective_date")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期（如已废止）", example = "2030-12-31")
    @TableField("expire_date")
    private LocalDate expireDate;

    @Schema(description = "法规效力状态：EFFECTIVE 现行有效 / REVISING 修订中 / OBSOLETE 已废止",
        example = "EFFECTIVE", allowableValues = {"EFFECTIVE", "REVISING", "OBSOLETE"})
    @TableField("status")
    private String status;

    @Schema(description = "法规全文")
    @TableField("full_text")
    private String fullText;

    @Schema(description = "摘要", example = "为规范网络数据处理活动，保障数据安全与合法权益")
    @TableField("digest")
    private String digest;

    @Schema(description = "原文 URL", example = "https://www.gov.cn/zhengce/2024-06/15/content_xxxxx.html")
    @TableField("source_url")
    private String sourceUrl;

    @Schema(description = "行政区划代码（仅地方政府规章有值）", example = "110000")
    @TableField("region_code")
    private String regionCode;

    @Schema(description = "是否由爬虫自动抓取：1 是 / 0 否", example = "1")
    @TableField("is_from_crawler")
    private Integer isFromCrawler;

    @Schema(description = "创建时间", example = "2024-01-01T12:00:00")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间", example = "2024-01-01T12:00:00")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}