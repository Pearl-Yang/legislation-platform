package com.legal.legislation.ai.qwen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Qwen chat 请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenChatRequest {
    private String                  model;
    private List<QwenMessage>       messages;
    private Double                  temperature;
    private Double                  topP;
    private Integer                 maxTokens;
    /** 是否流式 — DashScope OpenAI 兼容模式下,默认 false 即返回整段 JSON;后端走非流式即可 */
    @Builder.Default
    private Boolean                 stream = Boolean.FALSE;
}
