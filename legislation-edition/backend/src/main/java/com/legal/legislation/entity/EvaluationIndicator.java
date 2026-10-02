package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评估指标 - 模块五
 */
@Data
@TableName("evaluation_indicator")
@Schema(description = "评估指标")
public class EvaluationIndicator {

    public static final String DIM_LEGALITY   = "LEGALITY";   // 合法性
    public static final String DIM_EXECUTION  = "EXECUTION";  // 落实性
    public static final String DIM_SATISFACTION = "SATISFACTION"; // 满意度

    public static final String UNIT_RATIO   = "RATIO";   // 比率
    public static final String UNIT_ABSOLUTE = "ABSOLUTE"; // 绝对值
    public static final String UNIT_SCORE   = "SCORE";   // 分数

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "指标中文名", example = "行政处罚案件公开率")
    @TableField("indicator_name")
    private String indicatorName;

    @Schema(description = "所属维度：LEGALITY 合法性 / EXECUTION 落实性 / SATISFACTION 满意度",
        example = "EXECUTION", allowableValues = {"LEGALITY", "EXECUTION", "SATISFACTION"})
    @TableField("dimension")
    private String dimension;

    @Schema(description = "权重（0~1）", example = "0.30")
    @TableField("weight")
    private java.math.BigDecimal weight;

    @Schema(description = "计算公式表达式")
    @TableField("formula")
    private String formula;

    @Schema(description = "数据来源", example = "司法部业务系统")
    @TableField("data_source")
    private String dataSource;

    @Schema(description = "单位：RATIO 比率 / ABSOLUTE 绝对值 / SCORE 分数",
        example = "RATIO", allowableValues = {"RATIO", "ABSOLUTE", "SCORE"})
    @TableField("unit")
    private String unit;

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