package com.legal.legislation.task;

import com.legal.legislation.service.CleanupService;
import com.legal.legislation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 异步任务统一执行器。
 *
 * <p>使用 {@link TransactionalEventListener} 在业务事务 <b>提交后</b>
 * 触发,并标注 {@link Async} 让监听器在新线程执行,真正实现:
 * <ul>
 *   <li>业务 Service 的事务能正确提交/回滚</li>
 *   <li>耗时操作不阻塞 HTTP 响应</li>
 *   <li>避免 @Async 在同类自调用时被 Spring proxy bypass 的坑</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskRunner {

    private final CleanupService    cleanupService;
    private final EvaluationService evaluationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAsyncTask(AsyncTaskEvent event) {
        try {
            switch (event.getKind()) {
                case CLEANUP_RUN -> {
                    log.info("[AsyncTaskRunner] 开始执行清理任务 taskId={}", event.getTaskId());
                    cleanupService.runTask(event.getTaskId());
                    log.info("[AsyncTaskRunner] 清理任务 taskId={} 执行完成", event.getTaskId());
                }
                case EVALUATION_RUN -> {
                    log.info("[AsyncTaskRunner] 开始执行评估任务 taskId={}", event.getTaskId());
                    evaluationService.runTask(event.getTaskId());
                    log.info("[AsyncTaskRunner] 评估任务 taskId={} 执行完成", event.getTaskId());
                }
                default -> log.warn("[AsyncTaskRunner] 未知事件类型 {}", event.getKind());
            }
        } catch (Exception ex) {
            log.error("[AsyncTaskRunner] 异步任务执行失败 kind={} taskId={}", event.getKind(), event.getTaskId(), ex);
        }
    }
}
