package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.ReviewRule;
import com.legal.legislation.service.ReviewService;
import com.legal.legislation.service.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;

/**
 * 智慧审查 Controller - 模块三
 */
@Validated
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
@Tag(name = "03-智慧审查", description = "基于规则引擎与 AI 的智慧审查，红/黄/蓝/灰四级问题分类")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交审查任务",
        description = "对指定 draft_id 触发审查；审查完成会更新 review_record 与 review_issue。")
    @PostMapping("/submit")
    public Result<?> submit(@RequestBody Map<String, Object> body) {
        Long draftId    = toLong(body.get("draftId"));
        String type     = toString(body.get("reviewType"));
        if (draftId == null) return Result.error(400, "draftId 必填");
        return wrap(reviewService.submitReview(draftId, type));
    }

    @Operation(summary = "查询审查结果",
        description = "返回整体通过情况 + 问题列表（按严重级别排序）。")
    @GetMapping("/record/{id}")
    public Result<?> record(@PathVariable Long id) {
        return wrap(reviewService.getReviewRecord(id));
    }

    @Operation(summary = "列出所有审查规则",
        description = "前端管理后台用于规则启用 / 禁用 / 编辑。")
    @GetMapping("/rule-list")
    public Result<?> ruleList(
        @RequestParam(required = false) String ruleType,
        @RequestParam(required = false) String severity) {
        return wrap(reviewService.listRules(ruleType, severity));
    }

    @Operation(summary = "更新审查规则",
        description = "仅 admin 角色可用。")
    @PutMapping("/rule/{id}")
    public Result<?> updateRule(@PathVariable Long id, @RequestBody ReviewRule rule) {
        return wrap(reviewService.updateRule(id, rule));
    }

    @Operation(summary = "把审查问题标记为已解决")
    @PostMapping("/issue/{id}/resolve")
    public Result<?> resolveIssue(@PathVariable Long id) {
        return wrap(reviewService.resolveIssue(id));
    }

    @Operation(summary = "一键审查最近 N 条草案",
        description = "用于后台批量预审。")
    @PostMapping("/batch-submit")
    public Result<?> batchSubmit(@RequestBody List<Long> draftIds) {
        return wrap(reviewService.batchSubmit(draftIds));
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
    private static String toString(Object o) { return o == null ? null : o.toString(); }

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