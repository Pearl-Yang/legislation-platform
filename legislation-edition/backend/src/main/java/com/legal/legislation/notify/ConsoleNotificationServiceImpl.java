package com.legal.legislation.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 控制台通知实现（开发期默认）。
 *
 * 后续要做的事情：
 *   1. 写表 notify_message，落库以便"消息中心"页面展示
 *   2. 接入 WebSocket，把 message 推给 recipientId 对应的会话
 *   3. 邮件 / 短信接入（外部 channel 提供）
 */
@Slf4j
@Service
public class ConsoleNotificationServiceImpl implements NotifyService {

    @Override
    public boolean send(NotifyMessage message) {
        if (message == null) {
            return false;
        }
        log.info("[Notify] type={} recipient={} title={} biz={}:{} content={}",
            message.getType(),
            message.getRecipientId(),
            message.getTitle(),
            message.getBizType(),
            message.getBizId(),
            message.getContent());
        return true;
    }
}