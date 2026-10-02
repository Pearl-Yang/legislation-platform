package com.legal.legislation.notify;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知消息统一结构。
 * 不直接落到 NotificationService 内部，由调用方决定存储与发送通道。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知消息")
public class NotifyMessage {

    @Schema(description = "接收人用户 ID")
    private Long recipientId;

    @Schema(description = "消息类型：DEADLINE 期限 / CLEANUP 清理 / EVALUATION 评估 / SYSTEM 系统")
    private String type;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息正文")
    private String content;

    @Schema(description = "关联业务实体类型，如 legislative_project / cleanup_task")
    private String bizType;

    @Schema(description = "关联业务实体 ID")
    private Long bizId;

    @Schema(description = "发送时间")
    private LocalDateTime sentAt = LocalDateTime.now();
}