package com.legal.legislation.ai.qwen;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Qwen 在线客户端 - 调用 DashScope OpenAI 兼容端点。
 *
 * 依赖: spring-boot-starter-webflux (项目已包含)
 * 配置:
 *   legislation:
 *     qwen:
 *       mode: online
 *       apiKey: ${QWEN_API_KEY:sk-xxx}
 *       baseUrl: https://dashscope.aliyuncs.com/compatible-mode
 *       model: qwen-plus
 *
 * 失败时回退到 OfflineQwenClient(本类不负责回退,由 QwenFacade 处理)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "legislation.qwen", name = "mode", havingValue = "online")
public class QwenApiClient implements QwenClient {

    @Value("${legislation.qwen.apiKey:}")
    private String apiKey;

    @Value("${legislation.qwen.baseUrl:https://dashscope.aliyuncs.com/compatible-mode}")
    private String baseUrl;

    @Value("${legislation.qwen.model:qwen-plus}")
    private String defaultModel;

    @Override
    public boolean isOnline() { return true; }

    @Override
    public String chat(QwenChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Qwen] apiKey 未配置,跳过调用");
            return null;
        }
        if (request.getModel() == null) request.setModel(defaultModel);
        try {
            WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
            QwenChatResponse resp = client.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(QwenChatResponse.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(e -> {
                    log.warn("[Qwen] 调用失败: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
            if (resp != null && resp.getChoices() != null && !resp.getChoices().isEmpty()) {
                return resp.getChoices().get(0).getMessage().getContent();
            }
            return null;
        } catch (Exception ex) {
            log.error("[Qwen] 异常", ex);
            return null;
        }
    }

    /** 简化的便捷调用 */
    public String quickChat(String systemPrompt, String userPrompt) {
        QwenChatRequest req = QwenChatRequest.builder()
            .model(defaultModel)
            .temperature(0.3)
            .topP(0.9)
            .maxTokens(2048)
            .messages(java.util.List.of(
                QwenMessage.system(systemPrompt),
                QwenMessage.user(userPrompt)
            ))
            .build();
        return chat(req);
    }

    /** 占位静态帮助,避免 import warning */
    @SuppressWarnings("unused")
    private static Map<String, String> headers() { return Map.of("X-Source", "legislation-platform"); }
}
