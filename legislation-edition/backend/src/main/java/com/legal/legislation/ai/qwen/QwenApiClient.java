package com.legal.legislation.ai.qwen;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
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
            log.warn("[Qwen] apiKey 未配置,跳过在线调用");
            return null;
        }
        if (request.getModel() == null) request.setModel(defaultModel);
        // 强制非流式 — 我们后端用整段 JSON 解析,不接 SSE
        request.setStream(Boolean.FALSE);
        try {
            WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
            QwenChatResponse resp = client.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(s -> s.isError(), r -> r.bodyToMono(String.class).defaultIfEmpty("")
                    .flatMap(body -> Mono.error(new WebClientResponseException(
                        r.statusCode().value(), "Qwen HTTP " + r.statusCode().value(), null,
                        body.getBytes(StandardCharsets.UTF_8), null))))
                .bodyToMono(QwenChatResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            if (resp != null && resp.getChoices() != null && !resp.getChoices().isEmpty()) {
                String content = resp.getChoices().get(0).getMessage().getContent();
                if (content == null) return null;
                return content;
            }
            return null;
        } catch (WebClientResponseException wex) {
            // HTTP 4xx/5xx — 把 DashScope 真实错误体写日志,不要吞
            log.error("[Qwen] HTTP {} body={}", wex.getStatusCode(), wex.getResponseBodyAsString());
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
