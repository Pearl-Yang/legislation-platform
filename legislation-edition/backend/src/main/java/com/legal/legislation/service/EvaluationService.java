package com.legal.legislation.service;

import com.legal.legislation.entity.EvaluationIndicator;
import com.legal.legislation.entity.EvaluationResult;
import com.legal.legislation.entity.EvaluationTask;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EvaluationService {

    Task<List<EvaluationTask>> listTasks(String status, Long regulationId);

    Task<EvaluationTask> createTask(EvaluationTask task);

    Task<Map<String, Object>> getTaskDetail(Long taskId);

    /** ECharts 雷达图 + 同期对比折线 */
    Task<Map<String, Object>> getChartData(Long taskId);

    Task<Map<String, Object>> exportReport(Long taskId, String format);

    Task<List<EvaluationIndicator>> listIndicators();

    /** 数据同步（占位：模拟拉取外部数据） */
    Task<Integer> syncData(LocalDate periodStart, LocalDate periodEnd);

    Task<List<Map<String, Object>>> compareByType(String regulationType, String period);

    /** 实际跑指标计算(被 AsyncTaskRunner 在事务提交后调用) */
    void runTask(Long taskId);

    void setEventPublisher(ApplicationEventPublisher publisher);
}