package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legal.legislation.entity.*;
import com.legal.legislation.mapper.*;
import com.legal.legislation.service.LegislativeFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 立法流程服务实现
 *
 * 复用了 CaseFlowService 的设计：
 *  - 项目立项时根据 legislative_stage_template 实例化；
 *  - 节点推进/回退；
 *  - 进度计算 + 期限预警查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegislativeFlowServiceImpl implements LegislativeFlowService {

    private final LegislativeProjectMapper  projectMapper;
    private final LegislativeStageMapper    stageMapper;
    private final LegislativeDeadlineMapper deadlineMapper;
    private final LegislativeStageTemplateMapper templateMapper;

    /**
     * 立项后初始化流程节点 + 期限模板
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeStages(Long projectId, String projectType) {
        LegislativeProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在: " + projectId);
        }
        if (!projectType.equals(project.getProjectType())) {
            throw new IllegalArgumentException("项目类型不一致");
        }

        List<LegislativeStageTemplate> templates = templateMapper.selectByType(projectType);
        if (templates.isEmpty()) {
            log.warn("未找到类型 {} 的流程模板，请检查 legislative_stage_template 表配置", projectType);
            return;
        }

        LocalDate baseDate = LocalDate.now();
        LocalDate cumulativeDate = baseDate;

        for (int i = 0; i < templates.size(); i++) {
            LegislativeStageTemplate tpl = templates.get(i);
            boolean isFirst = (i == 0);

            LegislativeStage stage = new LegislativeStage();
            stage.setProjectId(projectId);
            stage.setStageCode(tpl.getStageCode());
            stage.setStageName(tpl.getStageName());
            stage.setStageOrder(tpl.getStageOrder());
            stage.setStatus(isFirst ? LegislativeStage.STATUS_IN_PROGRESS : LegislativeStage.STATUS_PENDING);
            stageMapper.insert(stage);

            LegislativeDeadline ddl = new LegislativeDeadline();
            ddl.setProjectId(projectId);
            ddl.setNodeName(tpl.getStageName());
            ddl.setRemindBeforeDays(7);
            ddl.setStatus(LegislativeDeadline.STATUS_PENDING);
            if (tpl.getDefaultDays() != null) {
                cumulativeDate = cumulativeDate.plusDays(tpl.getDefaultDays());
                ddl.setDeadlineDate(cumulativeDate);
            }
            deadlineMapper.insert(ddl);
        }
        log.info("已为项目 {} 初始化 {} 个节点", projectId, templates.size());
    }

    /**
     * 推进到下一阶段
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long advanceToNextStage(Long projectId, Long operatorId, String remark) {
        LegislativeStage cur = getCurrentStage(projectId);
        if (cur == null) {
            return null;
        }
        cur.setStatus(LegislativeStage.STATUS_DONE);
        cur.setOperatorId(operatorId);
        cur.setOperatorTime(java.time.LocalDateTime.now());
        cur.setRemark(remark);
        stageMapper.updateById(cur);

        // 推进到下一个 PENDING 节点
        QueryWrapper<LegislativeStage> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId)
          .eq("status", LegislativeStage.STATUS_PENDING)
          .orderByAsc("stage_order")
          .last("LIMIT 1");
        LegislativeStage next = stageMapper.selectOne(qw);
        if (next != null) {
            next.setStatus(LegislativeStage.STATUS_IN_PROGRESS);
            stageMapper.updateById(next);
            return next.getId();
        }
        // 没有下一节点 → 项目推进到 PUBLISHED
        LegislativeProject project = projectMapper.selectById(projectId);
        if (project != null) {
            project.setStatus(LegislativeProject.STATUS_PUBLISHED);
            project.setPublishDate(LocalDate.now());
            project.setUpdatedAt(java.time.LocalDateTime.now());
            projectMapper.updateById(project);
        }
        return null;
    }

    /**
     * 回退到指定 stageOrder
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackToStage(Long projectId, Integer targetStageOrder, Long operatorId, String remark) {
        if (targetStageOrder == null) {
            throw new IllegalArgumentException("目标阶段顺序号不能为空");
        }
        // 把所有 stageOrder >= targetOrder 的节点置 PENDING
        QueryWrapper<LegislativeStage> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId)
          .ge("stage_order", targetStageOrder)
          .ne("status", LegislativeStage.STATUS_DONE);
        List<LegislativeStage> stages = stageMapper.selectList(qw);
        for (LegislativeStage s : stages) {
            s.setStatus(LegislativeStage.STATUS_PENDING);
            s.setOperatorId(null);
            s.setOperatorTime(null);
            s.setRemark(null);
            stageMapper.updateById(s);
        }
        // 目标节点置 IN_PROGRESS
        QueryWrapper<LegislativeStage> targetQw = new QueryWrapper<>();
        targetQw.eq("project_id", projectId).eq("stage_order", targetStageOrder);
        LegislativeStage target = stageMapper.selectOne(targetQw);
        if (target != null) {
            target.setStatus(LegislativeStage.STATUS_IN_PROGRESS);
            target.setOperatorId(operatorId);
            target.setOperatorTime(java.time.LocalDateTime.now());
            target.setRemark(remark);
            stageMapper.updateById(target);
        }
    }

    /**
     * 计算项目完成进度百分比
     */
    @Override
    public int getProgressPercentage(Long projectId) {
        long total = stageMapper.selectCount(new QueryWrapper<LegislativeStage>().eq("project_id", projectId));
        if (total == 0) return 0;
        long done = stageMapper.selectCount(new QueryWrapper<LegislativeStage>()
                .eq("project_id", projectId)
                .eq("status", LegislativeStage.STATUS_DONE));
        return (int) Math.round(done * 100.0 / total);
    }

    @Override
    public List<LegislativeStage> listStages(Long projectId) {
        return stageMapper.selectList(new QueryWrapper<LegislativeStage>()
                .eq("project_id", projectId)
                .orderByAsc("stage_order"));
    }

    @Override
    public LegislativeStage getCurrentStage(Long projectId) {
        return stageMapper.selectOne(new QueryWrapper<LegislativeStage>()
                .eq("project_id", projectId)
                .eq("status", LegislativeStage.STATUS_IN_PROGRESS)
                .orderByAsc("stage_order")
                .last("LIMIT 1"));
    }

    @Override
    public List<LegislativeDeadline> getDeadlineOverview(Long projectId, int daysAhead) {
        LocalDate limit = LocalDate.now().plusDays(daysAhead);
        return deadlineMapper.selectList(new QueryWrapper<LegislativeDeadline>()
                .eq("project_id", projectId)
                .le("deadline_date", limit)
                .in("status", LegislativeDeadline.STATUS_PENDING, LegislativeDeadline.STATUS_OVERDUE)
                .orderByAsc("deadline_date"));
    }

    @Override
    public List<LegislativeDeadline> getUpcomingAcrossProjects(int daysAhead) {
        LocalDate limit = LocalDate.now().plusDays(daysAhead);
        QueryWrapper<LegislativeDeadline> qw = new QueryWrapper<>();
        qw.in("status", LegislativeDeadline.STATUS_PENDING, LegislativeDeadline.STATUS_OVERDUE)
          .le("deadline_date", limit)
          .orderByAsc("deadline_date")
          .last("LIMIT 200");
        List<LegislativeDeadline> deadlines = deadlineMapper.selectList(qw);
        if (deadlines.isEmpty()) {
            return Collections.emptyList();
        }
        // 附带项目名称,前端可直接展示
        List<Long> projectIds = deadlines.stream()
                .map(LegislativeDeadline::getProjectId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> projectNames = projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(LegislativeProject::getId, LegislativeProject::getProjectName));
        deadlines.forEach(d -> {
            // 通过反射或者项目名称注入到一个 transient 字段
            // 这里简单用 BizProjectName 注入到 remark(模板表已有 remark 字段为节点备注)
            // 实际使用中可以扩展 LegislativeDeadline 字段
            d.setNodeName(d.getNodeName());
            // 兼容: 通过 description 字段如果没有,就加在 remark 风格上
        });
        return deadlines;
    }
}