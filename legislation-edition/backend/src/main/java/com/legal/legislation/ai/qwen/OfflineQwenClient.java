package com.legal.legislation.ai.qwen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Qwen 离线占位实现。
 *
 * 始终返回 null;上层 QwenFacade 在收到 null 时回退到本地规则引擎
 * (SuperiorLawParser / ReviewEngine / CleanupAdvisor 等已在 P1 阶段实现)。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "legislation.qwen", name = "mode", havingValue = "offline", matchIfMissing = true)
public class OfflineQwenClient implements QwenClient {

    @Override public boolean isOnline() { return false; }

    @Override
    public String chat(QwenChatRequest request) {
        log.debug("[Qwen-offline] 跳过调用,使用本地规则引擎");
        return null;
    }
}
