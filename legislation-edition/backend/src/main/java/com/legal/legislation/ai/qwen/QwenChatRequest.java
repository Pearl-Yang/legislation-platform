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
}
