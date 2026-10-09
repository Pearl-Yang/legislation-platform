package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.LibraryMaterial;
import com.legal.legislation.service.LibraryService;
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
 * 立法资料库 Controller - 模块七
 */
@Validated
@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
@Tag(name = "07-立法资料库", description = "立法资料：法规 / 草案 / 报告 / 专家意见 / 典型案例")
public class LibraryController {

    private final LibraryService libraryService;

    @Operation(summary = "分页查询资料")
    @GetMapping("/material/list")
    public Result<?> list(
        @RequestParam(defaultValue = "1")  int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String materialType,
        @RequestParam(required = false) String regionCode,
        @RequestParam(required = false) String keyword) {
        return wrap(libraryService.list(page, size, materialType, regionCode, keyword));
    }

    @Operation(summary = "新增资料")
    @PostMapping("/material")
    public Result<?> create(@RequestBody LibraryMaterial body) {
        return wrap(libraryService.create(body));
    }

    @Operation(summary = "更新资料")
    @PutMapping("/material/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody LibraryMaterial body) {
        return wrap(libraryService.update(id, body));
    }

    @Operation(summary = "删除资料")
    @DeleteMapping("/material/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return wrap(libraryService.delete(id));
    }

    @Operation(summary = "关键词全文检索")
    @GetMapping("/material/search")
    public Result<?> search(
        @RequestParam String keyword,
        @RequestParam(required = false) String materialType,
        @RequestParam(defaultValue = "1")  int page,
        @RequestParam(defaultValue = "20") int size) {
        return wrap(libraryService.search(keyword, materialType, page, size));
    }

    @Operation(summary = "资料详情")
    @GetMapping("/material/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return wrap(libraryService.detail(id));
    }

    @Operation(summary = "收藏资料")
    @PostMapping("/material/{id}/favorite")
    public Result<?> favorite(
        @PathVariable Long id,
        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) userId = 1L;
        return wrap(libraryService.favorite(id, userId));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/material/{id}/favorite")
    public Result<?> unFavorite(
        @PathVariable Long id,
        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) userId = 1L;
        return wrap(libraryService.unFavorite(id, userId));
    }

    @Operation(summary = "我的收藏列表")
    @GetMapping("/material/favorites")
    public Result<?> myFavorites(
        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) userId = 1L;
        return wrap(libraryService.myFavorites(userId));
    }

    @Operation(summary = "新增批注")
    @PostMapping("/material/{id}/note")
    public Result<?> note(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body,
        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) userId = 1L;
        String content = body.get("content") == null ? null : body.get("content").toString();
        String highlighted = body.get("highlightedText") == null ? "" : body.get("highlightedText").toString();
        return wrap(libraryService.addNote(id, userId, content, highlighted));
    }

    @Operation(summary = "查询某资料的批注列表")
    @GetMapping("/material/{id}/notes")
    public Result<?> notes(@PathVariable Long id) {
        return wrap(libraryService.listNotes(id));
    }

    @Operation(summary = "查询相关推荐资料")
    @GetMapping("/material/{id}/related")
    public Result<?> related(@PathVariable Long id) {
        return wrap(libraryService.related(id));
    }

    @Operation(summary = "批量导入资料（爬虫写入入口）")
    @PostMapping("/material/batch-import")
    public Result<?> batchImport(@RequestBody List<LibraryMaterial> body) {
        return wrap(libraryService.batchImport(body));
    }

    @Operation(summary = "查询所有可用标签")
    @GetMapping("/tag/list")
    public Result<?> tags() {
        return wrap(libraryService.listTags());
    }

    @Operation(summary = "搜索建议(自动补全)")
    @GetMapping("/material/suggest")
    public Result<?> suggest(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "10") int limit) {
        return wrap(libraryService.suggest(keyword, limit));
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