package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.Regulation;
import com.legal.legislation.service.RegulationService;
import com.legal.legislation.service.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

/**
 * 法规 Controller - 模块四 / 模块七 共用
 */
@Validated
@RestController
@RequestMapping("/regulation")
@RequiredArgsConstructor
@Tag(name = "04-智能清理", description = "法规主数据 / 上下位关系 / 全文检索")
public class RegulationController {

    private final RegulationService regulationService;

    @Operation(summary = "分页查询法规",
        description = "支持按类型（ADMIN_REGULATION/DEPT_RULE/LOCAL_RULE）、状态、地区过滤。")
    @GetMapping("/list")
    public Result<?> list(
        @RequestParam(defaultValue = "1")  int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String regulationType,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String regionCode) {
        return wrap(regulationService.list(page, size, regulationType, status, regionCode));
    }

    @Operation(summary = "关键词全文检索",
        description = "同时匹配 regulation_name / digest / full_text。")
    @GetMapping("/search")
    public Result<?> search(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "1")  int page,
        @RequestParam(defaultValue = "20") int size) {
        return wrap(regulationService.search(keyword, page, size));
    }

    @Operation(summary = "查询单个法规详情")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return wrap(regulationService.getDetail(id));
    }

    @Operation(summary = "新增或更新法规")
    @PostMapping
    public Result<?> upsert(@RequestBody Regulation body) {
        return wrap(regulationService.upsert(body));
    }

    @Operation(summary = "查询某法规的上下位 / 引用关系",
        description = "返回 nodes + edges，供前端图谱可视化使用。")
    @GetMapping("/{id}/relations")
    public Result<?> relations(
        @PathVariable Long id,
        @RequestParam(defaultValue = "2") int depth) {
        return wrap(regulationService.getRelations(id, depth));
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