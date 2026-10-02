package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.Consultation;
import com.legal.legislation.entity.Opinion;
import com.legal.legislation.entity.OpinionReply;
import com.legal.legislation.service.ConsultationService;
import com.legal.legislation.service.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 意见征集 Controller - 模块六
 */
@RestController
@RequestMapping("/consultation")
@RequiredArgsConstructor
@Tag(name = "06-意见征集", description = "公众意见征集：发布 / 提交 / 去重 / 分类 / 报告")
public class ConsultationController {

    private final ConsultationService consultationService;

    @Operation(summary = "列出征集公告")
    @GetMapping("/list")
    public Result<?> list(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1")  int page,
        @RequestParam(defaultValue = "20") int size) {
        return wrap(consultationService.list(page, size, status));
    }

    @Operation(summary = "新建征集公告")
    @PostMapping
    public Result<?> create(@RequestBody Consultation body) {
        return wrap(consultationService.create(body));
    }

    @Operation(summary = "征集详情")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return wrap(consultationService.getDetail(id));
    }

    @Operation(summary = "更新征集")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Consultation body) {
        return wrap(consultationService.update(id, body));
    }

    @Operation(summary = "公开提交意见（无需登录）")
    @PostMapping("/{id}/opinion")
    public Result<?> submitOpinion(@PathVariable Long id, @RequestBody Opinion body) {
        return wrap(consultationService.submitOpinion(id, body));
    }

    @Operation(summary = "列出某征集的所有意见（分页）")
    @GetMapping("/{id}/opinions")
    public Result<?> opinions(
        @PathVariable Long id,
        @RequestParam(defaultValue = "1")  int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String category) {
        return wrap(consultationService.listOpinions(id, page, size, status, category));
    }

    @Operation(summary = "意见分类统计(按类别 + 立场)")
    @GetMapping("/{id}/statistics")
    public Result<?> statistics(@PathVariable Long id) {
        return wrap(consultationService.getStatistics(id));
    }

    @Operation(summary = "关键词词云(echarts-wordcloud 友好)")
    @GetMapping("/{id}/wordcloud")
    public Result<?> wordCloud(
        @PathVariable Long id,
        @Parameter(description = "返回前 N 个词,默认 50,最大 200")
        @RequestParam(defaultValue = "50") int topN) {
        return wrap(consultationService.wordCloud(id, topN));
    }

    @Operation(summary = "触发 AI 自动归类")
    @PostMapping("/{id}/classify")
    public Result<?> classify(@PathVariable Long id) {
        return wrap(consultationService.classify(id));
    }

    @Operation(summary = "触发意见去重（SimHash + Embedding）")
    @PostMapping("/{id}/dedup")
    public Result<?> dedup(@PathVariable Long id) {
        return wrap(consultationService.dedup(id));
    }

    @Operation(summary = "导出征集报告")
    @GetMapping("/{id}/report")
    public Result<?> report(@PathVariable Long id) {
        return wrap(consultationService.exportReport(id));
    }

    @Operation(summary = "回复意见")
    @PostMapping("/opinion/{opinionId}/reply")
    public Result<?> replyOpinion(
        @PathVariable Long opinionId,
        @RequestBody Map<String, Object> body) {
        String content = body.get("content") == null ? null : body.get("content").toString();
        Long replyBy   = body.get("replyBy") == null ? null :
                Long.parseLong(body.get("replyBy").toString());
        return wrap(consultationService.replyOpinion(opinionId, content, replyBy));
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