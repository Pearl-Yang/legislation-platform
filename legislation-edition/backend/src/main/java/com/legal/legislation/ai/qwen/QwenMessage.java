package com.legal.legislation.ai.qwen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Qwen 消息体(对齐 OpenAI 协议,DashScope 兼容)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenMessage {
    private String role;   // system / user / assistant
    private String content;

    public static QwenMessage system(String c) { return QwenMessage.builder().role("system").content(c).build(); }
    public static QwenMessage user(String c)   { return QwenMessage.builder().role("user").content(c).build(); }
}
