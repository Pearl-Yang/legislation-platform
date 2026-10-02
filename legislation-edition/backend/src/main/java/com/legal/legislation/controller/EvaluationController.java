package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.EvaluationTask;
import com.legal.legislation.service.EvaluationService;
import com.legal.legislation.service.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 实施评估 Controller - 模块五
 */
@RestController
@RequestMapping("/evaluation")
@RequiredArgsConstructor
@Tag(name = "05-实施评估", description = "法规实施情况评估，合法性/落实性/满意度三维评分")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "列出评估任务")
    @GetMapping("/list")
    public Result<?> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long regulationId) {
        return wrap(evaluationService.listTasks(status, regulationId));
    }

    @Operation(summary = "新建评估任务",
        description = "传 regulationId + periodStart/End，自动按 evaluation_indicator 拉取指标。")
    @PostMapping
    public Result<?> submit(@RequestBody EvaluationTask body) {
        return wrap(evaluationService.createTask(body));
    }

    @Operation(summary = "查询评估任务详情")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return wrap(evaluationService.getTaskDetail(id));
    }

    @Operation(summary = "拉取评估图表数据（ECharts 友好）",
        description = "返回 3 维度雷达图数据 + 同期对比折线图。")
    @GetMapping("/{id}/chart-data")
    public Result<?> chart(@PathVariable Long id) {
        return wrap(evaluationService.getChartData(id));
    }

    @Operation(summary = "导出评估报告")
    @GetMapping("/{id}/report")
    public Result<?> report(
        @PathVariable Long id,
        @RequestParam(defaultValue = "HTML") String format) {
        return wrap(evaluationService.exportReport(id, format));
    }

    @Operation(summary = "查询指标库")
    @GetMapping("/indicator-list")
    public Result<?> indicators() {
        return wrap(evaluationService.listIndicators());
    }

    @Operation(summary = "触发数据同步（评估期初一次性跑）")
    @PostMapping("/data-sync")
    public Result<?> dataSync(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return wrap(evaluationService.syncData(periodStart, periodEnd));
    }

    @Operation(summary = "对比同类型法规的评估结果")
    @GetMapping("/compare")
    public Result<?> compare(
        @RequestParam String regulationType,
        @RequestParam String period) {
        return wrap(evaluationService.compareByType(regulationType, period));
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