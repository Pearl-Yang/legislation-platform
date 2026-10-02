package com.legal.legislation.ai.qwen;

/**
 * 抽象 Qwen 客户端。
 *
 * 真实实现: QwenApiClient   (调用 dashscope.aliyuncs.com)
 * 离线实现: OfflineQwenClient (本地规则引擎生成,本次 P1-2~P1-6 已用)
 *
 * 通过 legislation.qwen.mode 配置切换: online | offline(默认)
 */
public interface QwenClient {
    /** 同步 chat,返回 assistant 内容;不可用时返回 null */
    String chat(QwenChatRequest request);

    /** 是否在线模式 */
    boolean isOnline();
}
