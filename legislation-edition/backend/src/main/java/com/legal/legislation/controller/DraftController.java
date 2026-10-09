package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.DraftVersionHistory;
import com.legal.legislation.entity.LegislativeDraft;
import com.legal.legislation.service.DraftService;
import com.legal.legislation.service.Task;
import com.legal.legislation.service.util.ReportContentBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;

/**
 * 草案生成 Controller - 模块二
 * TODO: 对接 LLM（RAG + 异地规章检索）
 */
@Validated
@RestController
@RequestMapping("/draft")
@RequiredArgsConstructor
@Tag(name = "02-草案生成", description = "立法草案 AI 生成与版本管理")
public class DraftController {

    private final DraftService draftService;
    private final ReportContentBuilder reportBuilder;

    @Operation(summary = "提交草案生成任务（异步）",
        description = "立即返回 taskId，AI 在后台异步生成；可通过 /draft/task/{taskId} 轮询结果。")
    @PostMapping("/generate")
    public Result<?> generate(@RequestBody Map<String, Object> body) {
        Long   projectId = toLong(body.get("projectId"));
        Long   stageId   = toLong(body.get("stageId"));
        String prompt    = toString(body.get("prompt"));
        Long   createdBy = toLong(body.get("createdBy"));
        if (projectId == null) {
            return Result.error(400, "projectId 必填");
        }
        Task<String> t = draftService.submitGenerateTask(projectId, stageId, prompt, createdBy);
        return wrap(t);
    }

    @Operation(summary = "轮询生成进度",
        description = "返回 PROCESSING / SUCCESS / FAILED 三种状态；FAILED 时附错误信息。")
    @GetMapping("/task/{taskId}")
    public Result<?> taskStatus(@PathVariable String taskId) {
        Task<Map<String, Object>> t = draftService.getTaskStatus(taskId);
        return wrap(t);
    }

    @Operation(summary = "查询某项目下的所有草案",
        description = "按 version 倒序返回。")
    @GetMapping("/list")
    public Result<?> list(@RequestParam Long projectId) {
        Task<List<LegislativeDraft>> t = draftService.listByProject(projectId);
        return wrap(t);
    }

    @Operation(summary = "查询单个草案详情",
        description = "包含 version / 提示词快照 / 引用片段。")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return wrap(draftService.getDetail(id));
    }

    @Operation(summary = "提交人工修订",
        description = "修订后自动写入 draft_version_history；新版本号 = 老版本号 + 1。")
    @PostMapping("/{id}/revise")
    public Result<?> revise(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body) {
        String content     = toString(body.get("content"));
        String summary     = toString(body.get("changeSummary"));
        Long   changedBy   = toLong(body.get("changedBy"));
        return wrap(draftService.revise(id, content, summary, changedBy));
    }

    @Operation(summary = "查询某草案的历史版本列表")
    @GetMapping("/{id}/versions")
    public Result<?> versions(@PathVariable Long id) {
        Task<List<DraftVersionHistory>> t = draftService.listVersions(id);
        return wrap(t);
    }

    @Operation(summary = "导出草案（Markdown / DOCX / HTML）",
        description = "直接流式下载文件：MARKDOWN(text/markdown)、DOCX(application/vnd.openxmlformats-officedocument.wordprocessingml.document)、HTML(text/html)。")
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(
        @PathVariable Long id,
        @Parameter(description = "MARKDOWN | DOCX | HTML，默认 MARKDOWN")
        @RequestParam(defaultValue = "MARKDOWN") String format) {
        Task<LegislativeDraft> t = draftService.getDetail(id);
        if (t == null || !t.isSuccess() || t.getData() == null) {
            return ResponseEntity.status(404)
                    .body(("{\"code\":404,\"message\":\"" +
                            (t == null ? "Service returned null" : t.getMessage()) + "\"}")
                            .getBytes(StandardCharsets.UTF_8));
        }
        LegislativeDraft d = t.getData();
        String upper = format == null ? "MARKDOWN" : format.toUpperCase();
        String fileName = "draft-" + d.getId() + "-v" + d.getVersion() + "." + lowerExt(upper);

        byte[] bytes;
        MediaType mediaType;
        switch (upper) {
            case "DOCX" -> {
                bytes = reportBuilder.buildDraftDocx(d);
                mediaType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }
            case "HTML" -> {
                String md = reportBuilder.buildDraftMarkdown(d);
                bytes = reportBuilder.wrapHtml(
                        "立法草案 #" + d.getId() + "（第 " + d.getVersion() + " 版）", md)
                        .getBytes(StandardCharsets.UTF_8);
                mediaType = MediaType.parseMediaType("text/html;charset=utf-8");
            }
            default -> {
                bytes = reportBuilder.buildDraftMarkdown(d).getBytes(StandardCharsets.UTF_8);
                mediaType = MediaType.parseMediaType("text/markdown;charset=utf-8");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment",
                new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        headers.setContentLength(bytes.length);
        headers.set("X-Export-Format", upper);
        headers.set("Access-Control-Expose-Headers", "Content-Disposition,X-Export-Format");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private static String lowerExt(String format) {
        return switch (format) {
            case "DOCX" -> "docx";
            case "HTML" -> "html";
            default     -> "md";
        };
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private static String toString(Object o) {
        return o == null ? null : o.toString();
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
