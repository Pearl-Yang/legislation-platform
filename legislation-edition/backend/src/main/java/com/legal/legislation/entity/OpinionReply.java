package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 意见回复 - 模块六
 */
@Data
@TableName("opinion_reply")
@Schema(description = "意见回复")
public class OpinionReply {

    @Schema(description = "主键", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属意见 ID", example = "1")
    @TableField("opinion_id")
    private Long opinionId;

    @Schema(description = "回复内容", example = "感谢您的建议，我们将在二审稿中 ...")
    @TableField("reply_content")
    private String replyContent;

    @Schema(description = "回复人 ID", example = "1")
    @TableField("reply_by")
    private Long replyBy;

    @Schema(description = "回复时间", example = "2024-01-20T10:00:00")
    @TableField("reply_at")
    private LocalDateTime replyAt;

    @Schema(description = "是否已通知提交者：1 是 / 0 否", example = "1")
    @TableField("notify_sent")
    private Integer notifySent;
}