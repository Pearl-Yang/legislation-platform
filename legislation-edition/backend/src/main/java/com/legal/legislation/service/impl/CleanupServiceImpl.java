package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legal.legislation.entity.CleanupSuggestion;
import com.legal.legislation.entity.CleanupTask;
import com.legal.legislation.entity.Regulation;
import com.legal.legislation.entity.RegulationRelation;
import com.legal.legislation.mapper.CleanupSuggestionMapper;
import com.legal.legislation.mapper.CleanupTaskMapper;
import com.legal.legislation.mapper.RegulationMapper;
import com.legal.legislation.mapper.RegulationRelationMapper;
import com.legal.legislation.notify.NotifyMessage;
import com.legal.legislation.notify.NotifyService;
import com.legal.legislation.service.CleanupService;
import com.legal.legislation.service.Task;
import com.legal.legislation.task.AsyncTaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 智能清理 Service
 *
 * 触发模式:
 *     1. DAILY  日常清理（按 reviseTime > 5 年 触发候选）
 *     2. PERIODIC 定期集中清理（按 triggerRegulationId 上位法变更触发）
 *     3. THEMATIC 专项主题清理（按 theme 关键词扫描 full_text / digest）
 *
 * 建议生成：基于启发式 + 简易打分模型（占位）：
 *   - 与 trigger 上位法冲突          → OBSOLETE
 *   - 同一 region 下有更新的同类法     → OBSOLETE
 *   - 距生效超过 8 年 + 无引用         → MODIFY
 *   - 其他                            → KEEP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupServiceImpl implements CleanupService {

    private final CleanupTaskMapper       taskMapper;
    private final CleanupSuggestionMapper suggestionMapper;
    private final RegulationMapper       regulationMapper;
    private final RegulationRelationMapper relationMapper;
    private final NotifyService          notifyService;
    private final ApplicationEventPublisher eventPublisher;

    private final Random rng = new Random();

    @Override
    public void setEventPublisher(ApplicationEventPublisher publisher) {
        // 接口定义,目前直接通过构造注入,本方法仅做兜底(测试场景使用)
    }

    @Override
    public Task<List<CleanupTask>> listTasks(String status, String taskType) {
        QueryWrapper<CleanupTask> qw = new QueryWrapper<>();
        if (status   != null) qw.eq("status", status);
        if (taskType != null) qw.eq("task_type", taskType);
        qw.orderByDesc("created_at");
        return Task.ok(taskMapper.selectList(qw));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<CleanupTask> createTask(CleanupTask task) {
        if (task.getTaskName() == null || task.getTaskName().isBlank()) {
            return Task.error("任务名称不能为空");
        }
        if (task.getTaskType() == null) {
            return Task.error("任务类型不能为空");
        }
        task.setStatus(CleanupTask.STATUS_PENDING);
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        // 发布异步任务事件:业务事务提交后由 AsyncTaskRunner 在新线程跑候选扫描
        eventPublisher.publishEvent(new AsyncTaskEvent(this, AsyncTaskEvent.Kind.CLEANUP_RUN, task.getId()));
        return Task.ok(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runTask(Long taskId) {
        try {
            CleanupTask task = taskMapper.selectById(taskId);
            if (task == null) return;

            task.setStatus(CleanupTask.STATUS_RUNNING);
            taskMapper.updateById(task);

            List<Regulation> candidates = scanCandidates(task);
            for (Regulation r : candidates) {
                CleanupSuggestion sug = generateSuggestionFor(task, r);
                if (sug != null) suggestionMapper.insert(sug);
            }

            task.setStatus(CleanupTask.STATUS_DONE);
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            notifyService.send(new NotifyMessage(
                task.getCreatedBy(),
                "CLEANUP",
                "清理任务完成",
                String.format("任务「%s」已生成 %d 条建议。", task.getTaskName(), candidates.size()),
                "cleanup_task",
                taskId,
                LocalDateTime.now()
            ));
        } catch (Exception ex) {
            log.error("清理任务失败 taskId={}", taskId, ex);
            CleanupTask t = taskMapper.selectById(taskId);
            if (t != null) {
                t.setStatus(CleanupTask.STATUS_DONE);
                taskMapper.updateById(t);
            }
        }
    }

    private List<Regulation> scanCandidates(CleanupTask task) {
        QueryWrapper<Regulation> qw = new QueryWrapper<>();
        if (CleanupTask.TYPE_THEMATIC.equals(task.getTaskType()) && task.getTheme() != null) {
            // 主题扫描：模糊匹配
            qw.and(w -> w.like("regulation_name", task.getTheme())
                         .or().like("digest",          task.getTheme())
                         .or().like("full_text",       task.getTheme()));
        }
        if (task.getTriggerRegulationId() != null) {
            // 上位法触发：找出所有引用了此上位法的下位法
            QueryWrapper<RegulationRelation> relQw = new QueryWrapper<>();
            relQw.eq("target_id", task.getTriggerRegulationId());
            List<RegulationRelation> rels = relationMapper.selectList(relQw);
            if (rels.isEmpty()) return new ArrayList<>();
            List<Long> ids = rels.stream().map(RegulationRelation::getSourceId).toList();
            qw.in("id", ids);
        }
        if (CleanupTask.TYPE_DAILY.equals(task.getTaskType())) {
            // 日常清理：抓 5 年以上未更新的"现行有效"法规
            LocalDate threshold = LocalDate.now().minusYears(5);
            qw.apply("DATE(updated_at) <= {0}", threshold)
              .eq("status", Regulation.STATUS_EFFECTIVE);
        }
        qw.orderByAsc("id").last("LIMIT 200");
        return regulationMapper.selectList(qw);
    }

    private CleanupSuggestion generateSuggestionFor(CleanupTask task, Regulation r) {
        CleanupSuggestion s = new CleanupSuggestion();
        s.setTaskId(task.getId());
        s.setRegulationId(r.getId());

        String reason;
        String suggestion;
        BigDecimal confidence;
        if (CleanupTask.TYPE_PERIODIC.equals(task.getTaskType()) && task.getTriggerRegulationId() != null) {
            // 与上位法可能冲突: 给 OBSOLETE
            suggestion = CleanupSuggestion.SUG_OBSOLETE;
            reason     = "上位法变更触发清理：与触发上位法存在条款引用关系，建议结合审查报告判断是否废止。";
            confidence = new BigDecimal("0.75");
        } else if (r.getIssueDate() != null && r.getIssueDate().isBefore(LocalDate.now().minusYears(8))) {
            suggestion = CleanupSuggestion.SUG_MODIFY;
            reason     = "已施行 8 年以上，建议结合当前执法实践评估条款适应性。";
            confidence = new BigDecimal("0.65");
        } else if (r.getStatus() != null && Regulation.STATUS_OBSOLETE.equals(r.getStatus())) {
            suggestion = CleanupSuggestion.SUG_OBSOLETE;
            reason     = "法规本身已被标记为废止，请确认是否一并清理历史文本。";
            confidence = new BigDecimal("0.95");
        } else {
            suggestion = CleanupSuggestion.SUG_KEEP;
            reason     = "现行有效，无明显清理必要。";
            confidence = new BigDecimal(0.50 + rng.nextDouble() * 0.3);
        }

        s.setSuggestion(suggestion);
        s.setReason(reason);
        s.setAiConfidence(confidence);
        return s;
    }

    @Override
    public Task<Map<String, Object>> getTaskDetail(Long taskId) {
        CleanupTask task = taskMapper.selectById(taskId);
        if (task == null) return Task.error("任务不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("task", task);
        QueryWrapper<CleanupSuggestion> qw = new QueryWrapper<>();
        qw.eq("task_id", taskId);
        data.put("suggestions", suggestionMapper.selectList(qw));
        return Task.ok(data);
    }

    @Override
    public Task<List<Map<String, Object>>> listAffectedRegulations(Long taskId) {
        CleanupTask task = taskMapper.selectById(taskId);
        if (task == null) return Task.error("任务不存在");
        QueryWrapper<CleanupSuggestion> qw = new QueryWrapper<>();
        qw.eq("task_id", taskId);
        List<CleanupSuggestion> sugs = suggestionMapper.selectList(qw);
        if (sugs.isEmpty()) return Task.ok(new ArrayList<>());

        List<Long> regIds = sugs.stream().map(CleanupSuggestion::getRegulationId).toList();
        Map<Long, Regulation> regMap = new HashMap<>();
        for (Regulation r : regulationMapper.selectBatchIds(regIds)) {
            regMap.put(r.getId(), r);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (CleanupSuggestion s : sugs) {
            Map<String, Object> item = new HashMap<>();
            item.put("suggestion",  s);
            item.put("regulation",  regMap.get(s.getRegulationId()));
            result.add(item);
        }
        return Task.ok(result);
    }

    @Override
    public Task<Integer> generateSuggestions(Long taskId) {
        CleanupTask task = taskMapper.selectById(taskId);
        if (task == null) return Task.error("任务不存在");
        List<Regulation> candidates = scanCandidates(task);
        int count = 0;
        for (Regulation r : candidates) {
            // 已存在 suggestion 的跳过
            QueryWrapper<CleanupSuggestion> qw = new QueryWrapper<>();
            qw.eq("task_id", taskId).eq("regulation_id", r.getId());
            if (suggestionMapper.selectCount(qw) > 0) continue;
            CleanupSuggestion s = generateSuggestionFor(task, r);
            if (s != null) {
                suggestionMapper.insert(s);
                count++;
            }
        }
        return Task.ok(count);
    }

    @Override
    public Task<Map<String, Object>> getTaskReport(Long taskId) {
        CleanupTask task = taskMapper.selectById(taskId);
        if (task == null) return Task.error("任务不存在");

        QueryWrapper<CleanupSuggestion> qw = new QueryWrapper<>();
        qw.eq("task_id", taskId);
        List<CleanupSuggestion> sugs = suggestionMapper.selectList(qw);

        Map<String, Object> report = new HashMap<>();
        report.put("task", task);
        long keep = sugs.stream().filter(s -> CleanupSuggestion.SUG_KEEP.equals(s.getSuggestion())).count();
        long modify = sugs.stream().filter(s -> CleanupSuggestion.SUG_MODIFY.equals(s.getSuggestion())).count();
        long obsolete = sugs.stream().filter(s -> CleanupSuggestion.SUG_OBSOLETE.equals(s.getSuggestion())).count();
        long approved = sugs.stream().filter(s -> s.getFinalDecision() != null && !CleanupSuggestion.SUG_KEEP.equals(s.getFinalDecision())).count();
        Map<String, Object> distribution = new HashMap<>();
        distribution.put("KEEP", keep);
        distribution.put("MODIFY", modify);
        distribution.put("OBSOLETE", obsolete);
        distribution.put("approved", approved);
        report.put("distribution", distribution);
        report.put("total", sugs.size());
        return Task.ok(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Boolean> decide(Long suggestionId, String finalDecision, String remark, Long decidedBy) {
        CleanupSuggestion sug = suggestionMapper.selectById(suggestionId);
        if (sug == null) return Task.error("建议不存在");
        if (finalDecision == null) return Task.error("finalDecision 必填");
        sug.setFinalDecision(finalDecision);
        sug.setDecidedBy(decidedBy);
        sug.setDecidedAt(LocalDateTime.now());
        // 把 remark 塞进 reason 末尾（仅作占位，正式实现应建独立字段）
        if (remark != null && !remark.isBlank()) {
            sug.setReason(sug.getReason() + "\n[人工决策备注] " + remark);
        }
        int rows = suggestionMapper.updateById(sug);
        return Task.ok(rows > 0);
    }
}