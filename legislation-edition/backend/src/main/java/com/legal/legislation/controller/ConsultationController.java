package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.Consultation;
import com.legal.legislation.entity.Opinion;
import com.legal.legislation.entity.OpinionReply;
import com.legal.legislation.service.ConsultationService;
import com.legal.legislation.service.Task;
import com.legal.legislation.service.util.ReportContentBuilder;
import com.legal.legislation.entity.Consultation;
import com.legal.legislation.entity.Opinion;
import com.legal.legislation.entity.OpinionReply;
import com.legal.legislation.mapper.ConsultationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.validation.annotation.Validated;

/**
 * 意见征集 Controller - 模块六
 */
@Validated
@RestController
@RequestMapping("/consultation")
@RequiredArgsConstructor
@Tag(name = "06-意见征集", description = "公众意见征集：发布 / 提交 / 去重 / 分类 / 报告")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final ReportContentBuilder reportBuilder;
    private final ConsultationMapper consultationMapper;

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

    @Operation(summary = "导出征集报告",
        description = "流式下载：MARKDOWN(text/markdown)、DOCX(application/vnd.openxmlformats-officedocument.wordprocessingml.document)、HTML(text/html)。默认 HTML。")
    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> report(
        @PathVariable Long id,
        @Parameter(description = "MARKDOWN | DOCX | HTML，默认 HTML")
        @RequestParam(defaultValue = "HTML") String format) {
        Task<Map<String, Object>> stat = consultationService.getStatistics(id);
        if (stat == null || !stat.isSuccess() || stat.getData() == null) {
            return ResponseEntity.status(404)
                    .body(("{\"code\":404,\"message\":\"" +
                            (stat == null ? "Service returned null" : stat.getMessage()) + "\"}")
                            .getBytes(StandardCharsets.UTF_8));
        }
        Consultation c = consultationMapper.selectById(id);
        if (c == null) {
            return ResponseEntity.status(404)
                    .body("{\"code\":404,\"message\":\"征集不存在\"}".getBytes(StandardCharsets.UTF_8));
        }
        Map<String, Object> data = stat.getData();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> byCategory =
                (Map<String, Map<String, Integer>>) data.get("byCategory");
        Integer totalViews    = (Integer) data.get("totalViews");
        Integer totalOpinions = c.getTotalOpinions() == null ? 0 : c.getTotalOpinions();
        String bodyMd = "## 征集概况\n\n- 标题: " + c.getTitle() + "\n- 起止: " + c.getStartDate() + " ~ " + c.getEndDate() + "\n";
        String upper  = format == null ? "HTML" : format.toUpperCase();
        String fileName = "consultation-" + id + "." + lowerExt(upper);

        byte[] bytes;
        MediaType mediaType;
        switch (upper) {
            case "MARKDOWN" -> {
                bytes = reportBuilder.buildConsultationMarkdown(id, c.getTitle(), c.getStatus(),
                        totalViews, totalOpinions, byCategory, bodyMd)
                        .getBytes(StandardCharsets.UTF_8);
                mediaType = MediaType.parseMediaType("text/markdown;charset=utf-8");
            }
            case "DOCX" -> {
                bytes = reportBuilder.buildConsultationDocx(id, c.getTitle(), c.getStatus(),
                        totalViews, totalOpinions, byCategory, bodyMd);
                mediaType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }
            default -> {
                bytes = reportBuilder.wrapHtml(c.getTitle() + " · 意见征集报告", bodyMd)
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