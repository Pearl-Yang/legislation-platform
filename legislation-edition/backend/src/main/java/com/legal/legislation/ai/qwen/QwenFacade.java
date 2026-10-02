package com.legal.legislation.ai.qwen;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Qwen Facade - 在线调用 / 离线回退的统一入口。
 *
 * 业务侧只调这个类:
 *   qwenFacade.execute(prompt, () -> localFallbackString());
 *
 * 用法:
 *   String ans = qwenFacade.execute("起草条款", () -> parser.composeLocal(...));
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QwenFacade {

    private final QwenClient client;

    /** 拿不到在线答案时,fallback 到本地逻辑 */
    public String execute(String userPrompt, Supplier<String> offlineFallback) {
        QwenChatRequest req = QwenChatRequest.builder()
            .messages(java.util.List.of(
                QwenMessage.user(userPrompt)
            ))
            .build();
        String ans = client.chat(req);
        if (ans != null) return ans;
        log.info("[Qwen] 在线无应答或离线模式 → 走本地回退");
        return offlineFallback.get();
    }
}
