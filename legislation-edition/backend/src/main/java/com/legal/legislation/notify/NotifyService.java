package com.legal.legislation.notify;

/**
 * 通知服务统一接口。
 *
 * - 当前实现：{@link ConsoleNotificationServiceImpl}（开发期只打日志）
 * - 后续接入 legislation_commons 后，可换 WebSocket / 邮件 / 站内信 通道实现
 *
 * 不直接在 Service 中调 channel：保持业务层只关心"发一条通知"，通道是实现细节。
 */
public interface NotifyService {

    /**
     * 发送单条通知。失败时不应抛出异常，避免影响主业务。
     *
     * @return true=实际发送成功 / false=仅记录或被丢弃
     */
    boolean send(NotifyMessage message);
}