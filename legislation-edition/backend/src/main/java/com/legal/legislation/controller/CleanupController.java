package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.CleanupTask;
import com.legal.legislation.service.CleanupService;
import com.legal.legislation.service.Task;
import com.legal.legislation.service.util.ReportContentBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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
    private final ReportContentBuilder reportBuilder;

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

    @Operation(summary = "导出清理任务报告",
        description = "流式下载：MARKDOWN(text/markdown)、DOCX(application/vnd.openxmlformats-officedocument.wordprocessingml.document)、HTML(text/html)。默认 HTML。")
    @GetMapping("/task/{id}/report")
    public ResponseEntity<byte[]> report(
        @PathVariable Long id,
        @Parameter(description = "MARKDOWN | DOCX | HTML，默认 HTML")
        @RequestParam(defaultValue = "HTML") String format) {
        Task<Map<String, Object>> t = cleanupService.getTaskReport(id);
        if (t == null || !t.isSuccess() || t.getData() == null) {
            return ResponseEntity.status(404)
                    .body(("{\"code\":404,\"message\":\"" +
                            (t == null ? "Service returned null" : t.getMessage()) + "\"}")
                            .getBytes(StandardCharsets.UTF_8));
        }
        Map<String, Object> data = t.getData();
        CleanupTask task = (CleanupTask) data.get("task");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> affected = (List<Map<String, Object>>) data.get("affectedRegulations");
        int total = affected == null ? 0 : affected.size();
        int keep = 0, modify = 0, obsolete = 0;
        if (affected != null) {
            for (Map<String, Object> m : affected) {
                Object s = m.get("suggestion");
                String decision = s == null ? null : String.valueOf(((java.util.Map<?, ?>) s).get("decision"));
                if (decision == null) continue;
                switch (decision) {
                    case "KEEP"     -> keep++;
                    case "MODIFY"   -> modify++;
                    case "OBSOLETE" -> obsolete++;
                    default -> {}
                }
            }
        }
        String bodyMd = "## 任务概况\n\n- 任务名称: " + (task == null ? "" : task.getTaskName()) +
                "\n- 触发模式: " + (task == null ? "" : task.getTaskType()) +
                "\n- 状态: " + (task == null ? "" : task.getStatus()) + "\n";

        String upper  = format == null ? "HTML" : format.toUpperCase();
        String fileName = "cleanup-" + id + "." + lowerExt(upper);

        byte[] bytes;
        MediaType mediaType;
        switch (upper) {
            case "MARKDOWN" -> {
                bytes = reportBuilder.buildCleanupMarkdown(id,
                        task == null ? null : task.getTaskName(),
                        task == null ? null : task.getTaskType(),
                        task == null ? null : task.getStatus(),
                        total, keep, modify, obsolete, bodyMd).getBytes(StandardCharsets.UTF_8);
                mediaType = MediaType.parseMediaType("text/markdown;charset=utf-8");
            }
            case "DOCX" -> {
                bytes = reportBuilder.buildCleanupDocx(id,
                        task == null ? null : task.getTaskName(),
                        task == null ? null : task.getTaskType(),
                        task == null ? null : task.getStatus(),
                        total, keep, modify, obsolete, bodyMd);
                mediaType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }
            default -> {
                bytes = reportBuilder.wrapHtml(
                        (task == null ? "清理任务" : task.getTaskName()) + " · 清理报告", bodyMd)
                        .getBytes(StandardCharsets.UTF_8);
                mediaType = MediaType.parseMediaType("text/html;charset=utf-8");
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
        return switch (format == null ? "" : format.toUpperCase()) {
            case "MARKDOWN" -> "md";
            case "DOCX"     -> "docx";
            default         -> "html";
        };
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