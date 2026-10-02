package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 公众意见 - 模块六
 *
 * 状态机：NEW → PROCESSED → REPLIED
 */
@Data
@TableName("opinion")
@Schema(description = "公众意见")
public class Opinion {

    public static final String STATUS_NEW       = "NEW";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_REPLIED   = "REPLIED";

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属征集 ID", example = "1")
    @TableField("consultation_id")
    private Long consultationId;

    @Schema(description = "父意见 ID（用于回复上级意见的引用，可空）", example = "1")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "提交者姓名（可匿名提交）", example = "张三")
    @TableField("submitter_name")
    private String submitterName;

    @Schema(description = "提交者联系方式", example = "zhangsan@example.com")
    @TableField("submitter_contact")
    private String submitterContact;

    @Schema(description = "意见正文", example = "建议第二十条增加处罚力度 ...")
    @TableField("content")
    private String content;

    @Schema(description = "附件 URL 列表，JSON 数组")
    @TableField("attachment_urls")
    private String attachmentUrls;

    @Schema(description = "SimHash / embedding 哈希，用于去重", example = "8a3b12c4...")
    @TableField("similarity_hash")
    private String similarityHash;

    @Schema(description = "处理状态：NEW 新提交 / PROCESSED 已处理 / REPLIED 已回复",
        example = "NEW", allowableValues = {"NEW", "PROCESSED", "REPLIED"})
    @TableField("status")
    private String status;

    @Schema(description = "提交时间", example = "2024-01-15T10:00:00")
    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @Schema(description = "AI 自动归类的类别", example = "数据安全")
    @TableField("classified_category")
    private String classifiedCategory;

    @Schema(description = "AI 归类的置信度（0~1）", example = "0.92")
    @TableField("ai_category_confidence")
    private BigDecimal aiCategoryConfidence;

    @Schema(description = "处理人 ID", example = "1")
    @TableField("processed_by")
    private Long processedBy;

    @Schema(description = "处理时间", example = "2024-01-16T14:00:00")
    @TableField("processed_at")
    private LocalDateTime processedAt;
}