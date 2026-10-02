package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.CleanupTask;
import com.legal.legislation.service.CleanupService;
import com.legal.legislation.service.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 清理任务 Controller - 模块四
 */
@RestController
@RequestMapping("/cleanup")
@RequiredArgsConstructor
@Tag(name = "04-智能清理", description = "法规清理：日常 / 定期 / 专项 三种触发模式")
public class CleanupController {

    private final CleanupService cleanupService;

    @Operation(summary = "列出清理任务",
        description = "支持按 status / taskType 过滤。")
    @GetMapping("/task/list")
    public Result<?> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String taskType) {
        return wrap(cleanupService.listTasks(status, taskType));
    }

    @Operation(summary = "创建清理任务",
        description = "DAILY：每天定时跑；PERIODIC：单次；THEMATIC：专项主题清理。")
    @PostMapping("/task")
    public Result<?> create(@RequestBody CleanupTask body) {
        return wrap(cleanupService.createTask(body));
    }

    @Operation(summary = "查询任务详情")
    @GetMapping("/task/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return wrap(cleanupService.getTaskDetail(id));
    }

    @Operation(summary = "查询任务影响的候选法规列表")
    @GetMapping("/task/{id}/affected-regulations")
    public Result<?> affected(@PathVariable Long id) {
        return wrap(cleanupService.listAffectedRegulations(id));
    }

    @Operation(summary = "对候选法规生成 AI 建议（KEEP/MODIFY/OBSOLETE）")
    @PostMapping("/task/{id}/suggest")
    public Result<?> suggest(@PathVariable Long id) {
        return wrap(cleanupService.generateSuggestions(id));
    }

    @Operation(summary = "导出清理任务报告")
    @GetMapping("/task/{id}/report")
    public Result<?> report(@PathVariable Long id) {
        return wrap(cleanupService.getTaskReport(id));
    }

    @Operation(summary = "对单个建议做最终决策")
    @PostMapping("/suggestion/{id}/decide")
    public Result<?> decide(
        @PathVariable Long id,
        @RequestParam String finalDecision,
        @RequestParam(required = false) String remark,
        @RequestHeader(value = "X-User-Id", required = false) Long decidedBy) {
        return wrap(cleanupService.decide(id, finalDecision, remark, decidedBy));
    }

    private <T> Result<T> wrap(Task<T> t) {
        if (t == null) return Result.error(500, "Service returned null");
        if (!t.isSuccess()) return Result.error(t.getCode() == null ? 500 : t.getCode(), t.getMessage());
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("操作成功");
        r.setData(t.getData());
        return r;
    }
}