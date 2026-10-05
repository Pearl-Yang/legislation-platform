package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legal.legislation.entity.EvaluationIndicator;
import com.legal.legislation.entity.EvaluationResult;
import com.legal.legislation.entity.EvaluationTask;
import com.legal.legislation.entity.Regulation;
import com.legal.legislation.mapper.EvaluationIndicatorMapper;
import com.legal.legislation.mapper.EvaluationResultMapper;
import com.legal.legislation.mapper.EvaluationTaskMapper;
import com.legal.legislation.mapper.RegulationMapper;
import com.legal.legislation.notify.NotifyMessage;
import com.legal.legislation.notify.NotifyService;
import com.legal.legislation.service.EvaluationService;
import com.legal.legislation.service.Task;
import com.legal.legislation.task.AsyncTaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 实施评估 Service
 *
 * 评估流程:
 *   1. 创建评估任务 → 异步跑指标计算
 *   2. 按 evaluation_indicator 的 formula 模板做占位计算（无外部数据时使用占位分）
 *   3. 按 dimension 汇总 + 综合得分
 *   4. 生成图表 + 报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationTaskMapper       taskMapper;
    private final EvaluationResultMapper     resultMapper;
    private final EvaluationIndicatorMapper  indicatorMapper;
    private final RegulationMapper           regulationMapper;
    private final NotifyService              notifyService;
    private final ApplicationEventPublisher  eventPublisher;

    private final Random rng = new Random();

    @Override
    public void setEventPublisher(ApplicationEventPublisher publisher) {
        // 接口定义,目前直接通过构造注入,本方法仅做兜底(测试场景使用)
    }

    @Override
    public Task<List<EvaluationTask>> listTasks(String status, Long regulationId) {
        QueryWrapper<EvaluationTask> qw = new QueryWrapper<>();
        if (status       != null) qw.eq("status", status);
        if (regulationId != null) qw.eq("regulation_id", regulationId);
        qw.orderByDesc("created_at");
        return Task.ok(taskMapper.selectList(qw));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<EvaluationTask> createTask(EvaluationTask task) {
        if (task.getRegulationId() == null) {
            return Task.error("regulationId 必填");
        }
        if (regulationMapper.selectById(task.getRegulationId()) == null) {
            return Task.error("被评估的法规不存在");
        }
        task.setStatus(EvaluationTask.STATUS_PENDING);
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        // 发布异步任务事件:业务事务提交后由 AsyncTaskRunner 在新线程跑指标计算
        eventPublisher.publishEvent(new AsyncTaskEvent(this, AsyncTaskEvent.Kind.EVALUATION_RUN, task.getId()));
        return Task.ok(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runTask(Long taskId) {
        try {
            EvaluationTask task = taskMapper.selectById(taskId);
            if (task == null) return;

            task.setStatus(EvaluationTask.STATUS_RUNNING);
            taskMapper.updateById(task);

            // 清掉旧 results
            QueryWrapper<EvaluationResult> qw = new QueryWrapper<>();
            qw.eq("task_id", taskId);
            resultMapper.delete(qw);

            // 拉所有启用指标
            QueryWrapper<EvaluationIndicator> indQw = new QueryWrapper<>();
            indQw.eq("enabled", 1).orderByAsc("id");
            List<EvaluationIndicator> indicators = indicatorMapper.selectList(indQw);

            // 计算每个指标
            Map<String, BigDecimal> dimSumWeight = new HashMap<>();
            Map<String, BigDecimal> dimSumScore  = new HashMap<>();
            for (String d : List.of(
                    EvaluationIndicator.DIM_LEGALITY,
                    EvaluationIndicator.DIM_EXECUTION,
                    EvaluationIndicator.DIM_SATISFACTION)) {
                dimSumWeight.put(d, BigDecimal.ZERO);
                dimSumScore.put(d,  BigDecimal.ZERO);
            }

            for (EvaluationIndicator ind : indicators) {
                BigDecimal raw   = mockRawValue(ind, task);
                BigDecimal score = normalizeToScore(raw, ind);
                BigDecimal weight = ind.getWeight() == null ? BigDecimal.ZERO : ind.getWeight();

                EvaluationResult r = new EvaluationResult();
                r.setTaskId(taskId);
                r.setIndicatorId(ind.getId());
                r.setRawValue(raw);
                r.setNormalizedScore(score);
                r.setWeight(weight);
                r.setDataSnapshot(String.format("{\"raw\":%s,\"unit\":\"%s\"}",
                        raw == null ? "null" : raw.toPlainString(), ind.getUnit()));
                r.setPeriodLabel(periodLabel(task));
                resultMapper.insert(r);

                dimSumWeight.merge(ind.getDimension(), weight, BigDecimal::add);
                dimSumScore.merge(ind.getDimension(),
                        score.multiply(weight), BigDecimal::add);
            }

            // 维度得分
            Map<String, BigDecimal> dimScores = new HashMap<>();
            BigDecimal overall = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> e : dimSumWeight.entrySet()) {
                String dim = e.getKey();
                BigDecimal w = e.getValue();
                BigDecimal dimScore = w.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : dimSumScore.get(dim).divide(w, 3, RoundingMode.HALF_UP);
                dimScores.put(dim, dimScore);
                overall = overall.add(dimScore.multiply(w));
            }
            // 更新每个 result 的 dimension_score（同一维度内相同）
            List<EvaluationResult> results = resultMapper.selectList(qw);
            for (EvaluationResult r : results) {
                EvaluationIndicator ind = findIndicatorById(indicators, r.getIndicatorId());
                if (ind != null && dimScores.containsKey(ind.getDimension())) {
                    r.setDimensionScore(dimScores.get(ind.getDimension()));
                    resultMapper.updateById(r);
                }
            }

            task.setOverallScore(overall);
            task.setReportContent(buildReport(task, dimScores));
            task.setStatus(EvaluationTask.STATUS_COMPLETE);
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            notifyService.send(new NotifyMessage(
                task.getCreatedBy(),
                "EVALUATION",
                "评估任务完成",
                String.format("评估任务 #%d 已完成，综合得分 %s。", taskId, overall.setScale(2, RoundingMode.HALF_UP)),
                "evaluation_task",
                taskId,
                LocalDateTime.now()
            ));
        } catch (Exception ex) {
            log.error("评估任务失败 taskId={}", taskId, ex);
            EvaluationTask t = taskMapper.selectById(taskId);
            if (t != null) {
                t.setStatus(EvaluationTask.STATUS_COMPLETE);
                taskMapper.updateById(t);
            }
        }
    }

    /** 占位：未接入真实数据时，用随机 + 指标单位 模拟。 */
    private BigDecimal mockRawValue(EvaluationIndicator ind, EvaluationTask task) {
        // 简单模型：根据 regulationId + indicatorId 做种子产生稳定值
        long seed = (task.getRegulationId() == null ? 0L : task.getRegulationId()) * 1009L
                + ind.getId();
        Random r = new Random(seed);
        switch (ind.getUnit() == null ? "" : ind.getUnit()) {
            case EvaluationIndicator.UNIT_RATIO:
                return BigDecimal.valueOf(0.5 + r.nextDouble() * 0.4).setScale(3, RoundingMode.HALF_UP);
            case EvaluationIndicator.UNIT_ABSOLUTE:
                return BigDecimal.valueOf(50 + r.nextInt(500));
            case EvaluationIndicator.UNIT_SCORE:
            default:
                return BigDecimal.valueOf(60 + r.nextDouble() * 30).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal normalizeToScore(BigDecimal raw, EvaluationIndicator ind) {
        if (raw == null) return BigDecimal.ZERO;
        // 简化：比率 * 100；绝对值归一到 100；分数直接用
        return switch (ind.getUnit() == null ? "" : ind.getUnit()) {
            case EvaluationIndicator.UNIT_RATIO   -> raw.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            case EvaluationIndicator.UNIT_ABSOLUTE -> {
                // 简单：500 对应 100 分
                BigDecimal s = raw.multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(500), 2, RoundingMode.HALF_UP);
                yield s.min(BigDecimal.valueOf(100));
            }
            default -> raw.min(BigDecimal.valueOf(100));
        };
    }

    private String periodLabel(EvaluationTask task) {
        if (task.getPeriodStart() == null) return "未指定";
        int y = task.getPeriodStart().getYear();
        int q = (task.getPeriodStart().getMonthValue() - 1) / 3 + 1;
        return String.format("%dQ%d", y, q);
    }

    private EvaluationIndicator findIndicatorById(List<EvaluationIndicator> list, Long id) {
        return list.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
    }

    private String buildReport(EvaluationTask task, Map<String, BigDecimal> dimScores) {
        StringBuilder sb = new StringBuilder();
        Regulation reg = regulationMapper.selectById(task.getRegulationId());
        sb.append("# 法规实施评估报告\n\n");
        sb.append("- **法规名称**: ").append(reg == null ? "(未知)" : reg.getRegulationName()).append("\n");
        sb.append("- **评估周期**: ").append(task.getPeriodStart()).append(" ~ ").append(task.getPeriodEnd()).append("\n");
        sb.append("- **综合得分**: ").append(task.getOverallScore() == null ? "—" :
                task.getOverallScore().setScale(2, RoundingMode.HALF_UP)).append("\n\n");
        sb.append("## 三维度得分\n\n");
        sb.append("| 维度 | 得分 |\n|---|---|\n");
        for (Map.Entry<String, BigDecimal> e : dimScores.entrySet()) {
            sb.append("| ").append(translateDim(e.getKey())).append(" | ")
              .append(e.getValue().setScale(2, RoundingMode.HALF_UP)).append(" |\n");
        }
        return sb.toString();
    }

    private static String translateDim(String d) {
        return switch (d) {
            case EvaluationIndicator.DIM_LEGALITY      -> "合法性";
            case EvaluationIndicator.DIM_EXECUTION     -> "落实性";
            case EvaluationIndicator.DIM_SATISFACTION  -> "满意度";
            default -> d;
        };
    }

    @Override
    public Task<Map<String, Object>> getTaskDetail(Long taskId) {
        EvaluationTask task = taskMapper.selectById(taskId);
        if (task == null) return Task.error("任务不存在");
        QueryWrapper<EvaluationResult> qw = new QueryWrapper<>();
        qw.eq("task_id", taskId);
        List<EvaluationResult> results = resultMapper.selectList(qw);

        // 把 indicator 名字注入
        List<EvaluationIndicator> indicators = indicatorMapper.selectList(null);
        Map<Long, EvaluationIndicator> indMap = new HashMap<>();
        for (EvaluationIndicator i : indicators) indMap.put(i.getId(), i);

        List<Map<String, Object>> items = new ArrayList<>();
        for (EvaluationResult r : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("result",   r);
            item.put("indicator", indMap.get(r.getIndicatorId()));
            items.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("task",    task);
        data.put("results", items);
        return Task.ok(data);
    }

    @Override
    public Task<Map<String, Object>> getChartData(Long taskId) {
        EvaluationTask task = taskMapper.selectById(taskId);
        if (task == null) return Task.error("任务不存在");

        QueryWrapper<EvaluationResult> qw = new QueryWrapper<>();
        qw.eq("task_id", taskId);
        List<EvaluationResult> results = resultMapper.selectList(qw);

        List<EvaluationIndicator> indicators = indicatorMapper.selectList(null);
        Map<Long, EvaluationIndicator> indMap = new HashMap<>();
        for (EvaluationIndicator i : indicators) indMap.put(i.getId(), i);

        // 雷达图：维度 -> 平均归一化分
        Map<String, BigDecimal> dimScore = new HashMap<>();
        Map<String, Integer>    dimCount = new HashMap<>();
        for (EvaluationResult r : results) {
            EvaluationIndicator ind = indMap.get(r.getIndicatorId());
            if (ind == null) continue;
            dimScore.merge(ind.getDimension(), r.getNormalizedScore(), BigDecimal::add);
            dimCount.merge(ind.getDimension(), 1, Integer::sum);
        }
        List<Map<String, Object>> radar = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : dimScore.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("name",   translateDim(e.getKey()));
            point.put("max",    100);
            int c = dimCount.getOrDefault(e.getKey(), 1);
            point.put("value", e.getValue().divide(BigDecimal.valueOf(c), 2, RoundingMode.HALF_UP));
            radar.add(point);
        }
        // 同期对比折线:本任务 + 同 regulationId 最近 5 个已完成任务的综合得分
        List<Map<String, Object>> trend = new ArrayList<>();
        QueryWrapper<EvaluationTask> histQw = new QueryWrapper<>();
        histQw.eq("regulation_id", task.getRegulationId())
              .eq("status", EvaluationTask.STATUS_COMPLETE)
              .orderByAsc("completed_at")
              .last("LIMIT 5");
        List<EvaluationTask> history = taskMapper.selectList(histQw);
        for (EvaluationTask h : history) {
            Map<String, Object> pt = new HashMap<>();
            pt.put("period", periodLabel(h));
            pt.put("score",  h.getOverallScore());
            pt.put("date",   h.getCompletedAt());
            trend.add(pt);
        }
        // 如果没有历史,放当前任务自身
        if (trend.isEmpty()) {
            Map<String, Object> pt = new HashMap<>();
            pt.put("period", periodLabel(task));
            pt.put("score",  task.getOverallScore());
            pt.put("date",   task.getCompletedAt());
            trend.add(pt);
        }

        // 综合维度得分(直接给前端,无需聚合)
        Map<String, BigDecimal> dimScores = new HashMap<>();
        for (Map.Entry<String, BigDecimal> e : dimScore.entrySet()) {
            int c = dimCount.getOrDefault(e.getKey(), 1);
            dimScores.put(e.getKey(), e.getValue().divide(BigDecimal.valueOf(c), 2, RoundingMode.HALF_UP));
        }

        // 各指标明细(给"明细表"用)
        List<Map<String, Object>> indicatorList = new ArrayList<>();
        for (EvaluationResult r : results) {
            EvaluationIndicator ind = indMap.get(r.getIndicatorId());
            if (ind == null) continue;
            Map<String, Object> it = new HashMap<>();
            it.put("name",     ind.getIndicatorName());
            it.put("dimension", translateDim(ind.getDimension()));
            it.put("raw",      r.getRawValue());
            it.put("score",    r.getNormalizedScore());
            it.put("weight",   r.getWeight());
            it.put("unit",     ind.getUnit());
            indicatorList.add(it);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("radar",         radar);
        data.put("trend",         trend);
        data.put("dimScores",     dimScores);
        data.put("indicators",    indicatorList);
        data.put("overallScore",  task.getOverallScore());
        return Task.ok(data);
    }

    @Override
    public Task<Map<String, Object>> exportReport(Long taskId, String format) {
        EvaluationTask task = taskMapper.selectById(taskId);
        if (task == null) return Task.error("任务不存在");
        Map<String, Object> result = new HashMap<>();
        result.put("taskId",      taskId);
        result.put("format",      format == null ? "HTML" : format.toUpperCase());
        result.put("fileName",    "evaluation-" + taskId + "." + (format == null ? "html" : format.toLowerCase()));
        result.put("content",     task.getReportContent());
        result.put("overallScore", task.getOverallScore());
        result.put("exportedAt",  LocalDateTime.now());
        return Task.ok(result);
    }

    @Override
    public Task<List<EvaluationIndicator>> listIndicators() {
        QueryWrapper<EvaluationIndicator> qw = new QueryWrapper<>();
        qw.orderByAsc("id");
        return Task.ok(indicatorMapper.selectList(qw));
    }

    @Override
    public Task<Integer> syncData(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            return Task.error("periodStart / periodEnd 必填");
        }
        // 占位 - 实际接入时这里会触发对 case_info / reconsideration / survey 表的拉取
        log.info("[Evaluation] 占位: 数据同步 period={}~{}", periodStart, periodEnd);
        return Task.ok(0);
    }

    @Override
    public Task<List<Map<String, Object>>> compareByType(String regulationType, String period) {
        if (regulationType == null) return Task.error("regulationType 必填");

        // 找出该类型所有 regulation 各自最近一次评估的综合得分
        QueryWrapper<Regulation> regQw = new QueryWrapper<>();
        regQw.eq("regulation_type", regulationType);
        List<Regulation> regs = regulationMapper.selectList(regQw);
        if (regs.isEmpty()) return Task.ok(new ArrayList<>());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Regulation r : regs) {
            QueryWrapper<EvaluationTask> qw = new QueryWrapper<>();
            qw.eq("regulation_id", r.getId()).eq("status", EvaluationTask.STATUS_COMPLETE)
              .orderByDesc("completed_at").last("LIMIT 1");
            EvaluationTask t = taskMapper.selectOne(qw);
            if (t == null) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("regulationId",   r.getId());
            row.put("regulationName", r.getRegulationName());
            row.put("overallScore",  t.getOverallScore());
            row.put("completedAt",   t.getCompletedAt());
            row.put("periodLabel",   period);
            rows.add(row);
        }
        rows.sort(Comparator.comparing(
                (Map<String, Object> m) -> (BigDecimal) m.getOrDefault("overallScore", BigDecimal.ZERO)
        ).reversed());
        return Task.ok(rows);
    }
}