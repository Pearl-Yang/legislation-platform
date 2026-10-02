package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.legislation.entity.LegislativeProject;
import com.legal.legislation.mapper.LegislativeProjectMapper;
import com.legal.legislation.service.LegislativeFlowService;
import com.legal.legislation.service.LegislativeProjectService;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 立法项目 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegislativeProjectServiceImpl implements LegislativeProjectService {

    private final LegislativeProjectMapper projectMapper;
    private final LegislativeFlowService   flowService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<LegislativeProject> create(LegislativeProject project) {
        if (project.getProjectName() == null || project.getProjectName().isBlank()) {
            return Task.error("项目名称不能为空");
        }
        if (project.getProjectType() == null || project.getProjectType().isBlank()) {
            return Task.error("项目类型不能为空");
        }
        project.setStatus(LegislativeProject.STATUS_DRAFT);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        project.setIsDeleted(0);
        projectMapper.insert(project);
        // 立项后初始化流程节点
        flowService.initializeStages(project.getId(), project.getProjectType());
        // 草稿建好立即进入 ACTIVE 状态,便于仪表盘统计
        project.setStatus(LegislativeProject.STATUS_ACTIVE);
        projectMapper.updateById(project);
        return Task.ok(project);
    }

    @Override
    public Task<Map<String, Object>> getDetail(Long id) {
        LegislativeProject project = projectMapper.selectById(id);
        if (project == null) {
            return Task.error("项目不存在");
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("project", project);
        detail.put("stages", flowService.listStages(id));
        detail.put("currentStage", flowService.getCurrentStage(id));
        detail.put("deadlines", flowService.getDeadlineOverview(id, 90));
        detail.put("progress", flowService.getProgressPercentage(id));
        return Task.ok(detail);
    }

    @Override
    public Task<?> list(int page, int size, String status, String projectType) {
        QueryWrapper<LegislativeProject> qw = new QueryWrapper<>();
        if (status != null)       qw.eq("status", status);
        if (projectType != null)  qw.eq("project_type", projectType);
        qw.orderByDesc("created_at");
        Page<LegislativeProject> p = projectMapper.selectPage(new Page<>(page, size), qw);
        return Task.ok(p);
    }

    @Override
    @Cacheable(cacheNames = "project:dashboard", key = "'global'", sync = true)
    public Task<?> dashboard() {
        Map<String, Object> board = new HashMap<>();

        // === 状态分布 ===
        board.put("totalProjects", projectMapper.selectCount(null));
        board.put("active",    projectMapper.selectCount(qw(LegislativeProject.STATUS_ACTIVE,    null)));
        board.put("published", projectMapper.selectCount(qw(LegislativeProject.STATUS_PUBLISHED, null)));
        board.put("drafts",    projectMapper.selectCount(qw(LegislativeProject.STATUS_DRAFT,     null)));
        board.put("obsolete",  projectMapper.selectCount(qw(LegislativeProject.STATUS_OBSOLETE,  null)));

        // === 类型分布 ===
        Map<String, Object> typeDist = new HashMap<>();
        typeDist.put("ADMIN_REGULATION", projectMapper.selectCount(qw(null, "ADMIN_REGULATION")));
        typeDist.put("DEPT_RULE",        projectMapper.selectCount(qw(null, "DEPT_RULE")));
        typeDist.put("LOCAL_RULE",       projectMapper.selectCount(qw(null, "LOCAL_RULE")));
        board.put("typeDistribution", typeDist);

        // === 风险项目(即将到期 / 已逾期) ===
        try {
            java.util.List<?> upcoming = flowService.getUpcomingAcrossProjects(30);
            int overdue = 0;
            int dueSoon = 0;
            for (Object o : upcoming) {
                if (o instanceof java.util.Map<?, ?> m) {
                    Object overdueFlag = m.get("overdue");
                    if (Boolean.TRUE.equals(overdueFlag)) {
                        overdue++;
                    } else {
                        dueSoon++;
                    }
                }
            }
            board.put("riskProjects", overdue + dueSoon);
            board.put("overdueProjects", overdue);
            board.put("dueSoonProjects", dueSoon);
        } catch (Exception ex) {
            log.warn("dashboard 风险项目统计失败: {}", ex.getMessage());
            board.put("riskProjects", 0);
            board.put("overdueProjects", 0);
            board.put("dueSoonProjects", 0);
        }

        // === 最近活动(最近 10 条立项 / 推进) ===
        QueryWrapper<LegislativeProject> recentQw = new QueryWrapper<>();
        recentQw.orderByDesc("created_at").last("LIMIT 10");
        board.put("recentProjects", projectMapper.selectList(recentQw));

        return Task.ok(board);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Boolean> update(Long id, LegislativeProject project) {
        LegislativeProject exist = projectMapper.selectById(id);
        if (exist == null) {
            return Task.error("项目不存在");
        }
        project.setId(id);
        project.setUpdatedAt(LocalDateTime.now());
        // 不允许通过此接口修改 status,status 由 advance/rollback 控制
        project.setStatus(null);
        int rows = projectMapper.updateById(project);
        return Task.ok(rows > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Boolean> delete(Long id) {
        LegislativeProject exist = projectMapper.selectById(id);
        if (exist == null) {
            return Task.error("项目不存在");
        }
        int rows = projectMapper.deleteById(id);
        return Task.ok(rows > 0);
    }

    private QueryWrapper<LegislativeProject> qw(String status, String type) {
        QueryWrapper<LegislativeProject> q = new QueryWrapper<>();
        if (status != null) q.eq("status", status);
        if (type != null)    q.eq("project_type", type);
        return q;
    }
}