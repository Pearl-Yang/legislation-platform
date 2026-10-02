package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.LegislativeProject;
import com.legal.legislation.entity.LegislativeStageTemplate;
import com.legal.legislation.mapper.LegislativeStageTemplateMapper;
import com.legal.legislation.service.LegislativeFlowService;
import com.legal.legislation.service.LegislativeProjectService;
import com.legal.legislation.service.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 立法项目 Controller
 *
 * 模块一：行政立法项目全流程管理
 */
@RestController
@RequestMapping("/legislative-project")
@RequiredArgsConstructor
@Tag(name = "01-立法项目", description = "行政立法项目立项 / 流程推进 / 阶段模板 / 仪表盘")
public class LegislativeProjectController {

    private final LegislativeProjectService projectService;
    private final LegislativeFlowService   flowService;
    private final LegislativeStageTemplateMapper templateMapper;

    @Operation(summary = "分页查询立法项目",
        description = "支持按项目类型（ADMIN_REGULATION/DEPT_RULE/LOCAL_RULE）与状态（DRAFT/ACTIVE/PUBLISHED/OBSOLETE）筛选")
    @GetMapping("/list")
    public Result<?> list(
        @Parameter(description = "页码（从 1 开始）", example = "1") @RequestParam(defaultValue = "1")  int page,
        @Parameter(description = "每页条数", example = "20")           @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "项目类型：ADMIN_REGULATION/DEPT_RULE/LOCAL_RULE") @RequestParam(required = false) String projectType,
        @Parameter(description = "状态：DRAFT/ACTIVE/PUBLISHED/OBSOLETE")         @RequestParam(required = false) String status) {
        Task<?> t = projectService.list(page, size, status, projectType);
        return wrap(t);
    }

    @Operation(summary = "获取项目详情（含阶段、期限、进度等汇总）")
    @GetMapping("/{id}")
    public Result<?> detail(
        @Parameter(name = "id", description = "项目主键", required = true, example = "1")
        @PathVariable Long id) {
        Task<?> t = projectService.getDetail(id);
        return wrap(t);
    }

    @Operation(summary = "创建立法项目")
    @PostMapping
    public Result<?> create(@RequestBody LegislativeProject project) {
        Task<LegislativeProject> t = projectService.create(project);
        return wrap(t);
    }

    @Operation(summary = "更新立法项目基本信息",
        description = "注意：项目状态(status)请通过 /advance 或 /rollback 接口控制，不要在此处传入。")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody LegislativeProject project) {
        return wrap(projectService.update(id, project));
    }

    @Operation(summary = "逻辑删除立法项目")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return wrap(projectService.delete(id));
    }

    @Operation(summary = "推进至下一阶段",
        description = "根据流程模板自动计算下一阶段并落库，operatorId 取自 X-User-Id 头。")
    @PostMapping("/{id}/advance")
    public Result<?> advance(
        @PathVariable Long id,
        @Parameter(description = "操作人 ID（X-User-Id 头）", example = "1")
        @RequestHeader(value = "X-User-Id", required = false) Long operatorId,
        @Parameter(description = "备注", example = "完成征求意见，进入下一阶段")
        @RequestParam(required = false) String remark) {
        Long nextId = flowService.advanceToNextStage(id, operatorId, remark);
        Map<String, Object> data = new HashMap<>();
        data.put("nextStageId", nextId);
        data.put("completed",   nextId == null);
        return Result.success(data);
    }

    @Operation(summary = "回退到指定阶段",
        description = "回退到 stageOrder 对应的阶段；可用于审批驳回后重走流程。")
    @PostMapping("/{id}/rollback")
    public Result<?> rollback(
        @PathVariable Long id,
        @Parameter(description = "目标阶段顺序号", example = "3") Integer targetStageOrder,
        @Parameter(description = "操作人 ID（X-User-Id 头）", example = "1")
        @RequestHeader(value = "X-User-Id", required = false) Long operatorId,
        @Parameter(description = "回退原因", example = "起草不充分，驳回修改")
        @RequestParam(required = false) String remark) {
        try {
            flowService.rollbackToStage(id, targetStageOrder, operatorId, remark);
            return Result.success();
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        }
    }

    @Operation(summary = "查询项目全部流程节点（按 stageOrder 升序）")
    @GetMapping("/{id}/stages")
    public Result<?> listStages(@PathVariable Long id) {
        return Result.success(flowService.listStages(id));
    }

    @Operation(summary = "查询当前进行中的阶段")
    @GetMapping("/{id}/current-stage")
    public Result<?> currentStage(@PathVariable Long id) {
        return Result.success(flowService.getCurrentStage(id));
    }

    @Operation(summary = "获取项目整体进度百分比（0~100）")
    @GetMapping("/{id}/progress")
    public Result<?> progress(@PathVariable Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("progress", flowService.getProgressPercentage(id));
        return Result.success(data);
    }

    @Operation(summary = "查询项目未来 N 天的关键期限",
        description = "返回节点 + 截止日期 + 是否逾期，用于仪表盘与提醒。")
    @GetMapping("/{id}/deadlines")
    public Result<?> deadlines(
        @PathVariable Long id,
        @Parameter(description = "向前看天数", example = "30")
        @RequestParam(defaultValue = "30") int daysAhead) {
        return Result.success(flowService.getDeadlineOverview(id, daysAhead));
    }

    @Operation(summary = "项目仪表盘汇总数据",
        description = "返回进行中数量、各状态分布、风险项目数量等。")
    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        return wrap(projectService.dashboard());
    }

    @Operation(summary = "跨项目到期项目列表（即将到期 / 已逾期）",
        description = "用于工作台首页展示全局风险项目。")
    @GetMapping("/upcoming")
    public Result<?> upcoming(
        @Parameter(description = "向前看天数", example = "30")
        @RequestParam(defaultValue = "30") int daysAhead) {
        return Result.success(flowService.getUpcomingAcrossProjects(daysAhead));
    }

    @Operation(summary = "查询流程模板（公开接口，前端下拉使用）",
        description = "根据项目类型拉取对应的阶段模板（立项→起草→审查→征求意见→发布→评估→清理）。")
    @GetMapping("/stage-template/list")
    public Result<?> stageTemplates(
        @Parameter(description = "项目类型：ADMIN_REGULATION/DEPT_RULE/LOCAL_RULE", example = "ADMIN_REGULATION")
        @RequestParam(required = false) String type) {
        List<LegislativeStageTemplate> templates;
        if (type == null || type.isBlank()) {
            templates = templateMapper.selectList(null);
        } else {
            templates = templateMapper.selectByType(type);
        }
        return Result.success(templates);
    }

    private <T> Result<T> wrap(Task<T> t) {
        if (t == null) {
            return Result.error(500, "Service returned null");
        }
        if (!t.isSuccess()) {
            return Result.error(t.getCode() == null ? 500 : t.getCode(), t.getMessage());
        }
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("操作成功");
        r.setData(t.getData());
        return r;
    }
}