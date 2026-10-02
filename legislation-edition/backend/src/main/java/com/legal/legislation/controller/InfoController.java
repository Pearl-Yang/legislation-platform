package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.service.InfoService;
import com.legal.legislation.service.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 信息展示 Controller - 模块八
 */
@RestController
@RequestMapping("/info")
@RequiredArgsConstructor
@Tag(name = "08-立法信息展示", description = "立法动态 / 法规索引 / 政策解读 / 仪表盘")
public class InfoController {

    private final InfoService infoService;

    @Operation(summary = "立法动态列表")
    @GetMapping("/news")
    public Result<?> news(
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "1")  int page,
        @RequestParam(defaultValue = "20") int size) {
        return wrap(infoService.news(category, page, size));
    }

    @Operation(summary = "动态详情")
    @GetMapping("/news/{id}")
    public Result<?> newsDetail(@PathVariable Long id) {
        return wrap(infoService.newsDetail(id));
    }

    @Operation(summary = "法规索引（按行业 / 地区）")
    @GetMapping("/regulation-index")
    public Result<?> regulationIndex(
        @RequestParam(required = false) String domain,
        @RequestParam(required = false) String regionCode) {
        return wrap(infoService.regulationIndex(domain, regionCode));
    }

    @Operation(summary = "政策解读列表")
    @GetMapping("/policy-interpretations")
    public Result<?> policyInterpretations() {
        return wrap(infoService.policyInterpretations());
    }

    @Operation(summary = "学术文献列表")
    @GetMapping("/academic-literature")
    public Result<?> academicLiterature() {
        return wrap(infoService.academicLiterature());
    }

    @Operation(summary = "公报 / 简报")
    @GetMapping("/bulletin")
    public Result<?> bulletin() {
        return wrap(infoService.bulletin());
    }

    @Operation(summary = "仪表盘聚合数据")
    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        return wrap(infoService.dashboard());
    }

    @Operation(summary = "仪表盘图表数据（ECharts 友好）")
    @GetMapping("/dashboard/chart")
    public Result<?> dashboardChart() {
        return wrap(infoService.dashboardChart());
    }

    @Operation(summary = "订阅（邮件 / 微信 / 短信）")
    @PostMapping("/subscription")
    public Result<?> subscribe(@RequestBody Map<String, Object> body) {
        return wrap(infoService.subscribe(body));
    }

    @Operation(summary = "取消订阅")
    @DeleteMapping("/subscription/{id}")
    public Result<?> unsubscribe(@PathVariable Long id) {
        return wrap(infoService.unsubscribe(id));
    }

    @Operation(summary = "我的订阅列表")
    @GetMapping("/subscription/list")
    public Result<?> subscriptions(
        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return wrap(infoService.subscriptions(userId == null ? 1L : userId));
    }

    @Operation(summary = "根据资料 / 法规推荐相关内容")
    @GetMapping("/recommend")
    public Result<?> recommend(@RequestParam Long materialId) {
        return wrap(infoService.recommend(materialId));
    }

    @Operation(summary = "各省法规地图分布(供 ECharts 地图)")
    @GetMapping("/map/regulation")
    public Result<?> regulationMap() {
        return wrap(infoService.regulationMap());
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