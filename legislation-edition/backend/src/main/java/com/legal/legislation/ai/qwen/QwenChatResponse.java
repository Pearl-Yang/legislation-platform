package com.legal.legislation.ai.qwen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Qwen 响应体(只关心 choices)。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QwenChatResponse {
    private String id;
    private String model;
    private List<Choice> choices;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Integer    index;
        private QwenMessage message;
        private String     finishReason;
    }
}
