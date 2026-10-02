package com.legal.legislation.service;

import com.legal.legislation.entity.CleanupSuggestion;
import com.legal.legislation.entity.CleanupTask;

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
}