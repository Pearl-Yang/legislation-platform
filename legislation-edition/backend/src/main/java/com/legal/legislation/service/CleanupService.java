package com.legal.legislation.service;

import com.legal.legislation.entity.CleanupSuggestion;
import com.legal.legislation.entity.CleanupTask;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

public interface CleanupService {

    Task<List<CleanupTask>> listTasks(String status, String taskType);

    Task<CleanupTask> createTask(CleanupTask task);

    Task<Map<String, Object>> getTaskDetail(Long taskId);

    /** 当前任务已生成的所有候选法规（含它们的 suggestion） */
    Task<List<Map<String, Object>>> listAffectedRegulations(Long taskId);

    /** 触发 AI 建议生成（写入 cleanup_suggestion） */
    Task<Integer> generateSuggestions(Long taskId);

    /** 任务报告 */
    Task<Map<String, Object>> getTaskReport(Long taskId);

    /** 对单个建议做最终决策 */
    Task<Boolean> decide(Long suggestionId, String finalDecision, String remark, Long decidedBy);

    /**
     * 实际执行清理任务(被 AsyncTaskRunner 在事务提交后调用)。
     * 对外仅暴露给监听器;HTTP Controller 不应直接调用。
     */
    void runTask(Long taskId);

    /** 注入事件发布器(由实现类在构造期完成,本方法只用于测试) */
    void setEventPublisher(ApplicationEventPublisher publisher);
}