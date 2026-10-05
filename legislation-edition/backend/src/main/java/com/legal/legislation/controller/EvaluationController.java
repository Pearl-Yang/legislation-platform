package com.legal.legislation.controller;

import com.legal.legislation.common.Result;
import com.legal.legislation.entity.EvaluationTask;
import com.legal.legislation.service.EvaluationService;
import com.legal.legislation.service.Task;
import com.legal.legislation.service.util.ReportContentBuilder;
import com.legal.legislation.entity.Regulation;
import com.legal.legislation.entity.EvaluationIndicator;
import com.legal.legislation.entity.EvaluationResult;
import com.legal.legislation.mapper.EvaluationIndicatorMapper;
import com.legal.legislation.mapper.EvaluationResultMapper;
import com.legal.legislation.mapper.EvaluationTaskMapper;
import com.legal.legislation.mapper.RegulationMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
    private final ReportContentBuilder reportBuilder;
    private final RegulationMapper regulationMapper;
    private final EvaluationTaskMapper taskMapper;
    private final EvaluationResultMapper resultMapper;
    private final EvaluationIndicatorMapper indicatorMapper;

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

    @Operation(summary = "导出评估报告",
        description = "流式下载：MARKDOWN(text/markdown)、DOCX(application/vnd.openxmlformats-officedocument.wordprocessingml.document)、HTML(text/html)。")
    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> report(
        @PathVariable Long id,
        @Parameter(description = "MARKDOWN | DOCX | HTML，默认 HTML")
        @RequestParam(defaultValue = "HTML") String format) {
        EvaluationTask task = taskMapper.selectById(id);
        if (task == null) {
            return ResponseEntity.status(404)
                    .body(("{\"code\":404,\"message\":\"评估任务不存在\"}")
                            .getBytes(StandardCharsets.UTF_8));
        }
        Regulation reg = regulationMapper.selectById(task.getRegulationId());
        String regulationName = reg == null ? "法规#" + task.getRegulationId() : reg.getRegulationName();
        String periodStart = task.getPeriodStart() == null ? "" : task.getPeriodStart().toString();
        String periodEnd   = task.getPeriodEnd()   == null ? "" : task.getPeriodEnd().toString();
        String overall     = task.getOverallScore() == null ? "—" :
                task.getOverallScore().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        String periodLabel = (task.getPeriodStart() == null) ? "未指定" :
                (task.getPeriodStart().getYear() + "Q" + ((task.getPeriodStart().getMonthValue() - 1) / 3 + 1));

        // 维度分:从 results 聚合
        Map<String, java.math.BigDecimal> dimSum  = new java.util.HashMap<>();
        Map<String, Integer>               dimCnt  = new java.util.HashMap<>();
        List<EvaluationIndicator> inds = indicatorMapper.selectList(null);
        java.util.Map<Long, EvaluationIndicator> indMap = new java.util.HashMap<>();
        for (EvaluationIndicator i : inds) indMap.put(i.getId(), i);
        List<EvaluationResult> results = resultMapper.selectList(
                new QueryWrapper<EvaluationResult>().eq("task_id", id));
        for (EvaluationResult r : results) {
            EvaluationIndicator ind = indMap.get(r.getIndicatorId());
            if (ind == null) continue;
            dimSum.merge(ind.getDimension(), r.getNormalizedScore(), java.math.BigDecimal::add);
            dimCnt.merge(ind.getDimension(), 1, Integer::sum);
        }
        Map<String, String> dimScores = new LinkedHashMap<>();
        for (Map.Entry<String, java.math.BigDecimal> e : dimSum.entrySet()) {
            int c = dimCnt.getOrDefault(e.getKey(), 1);
            dimScores.put(translateDim(e.getKey()),
                    e.getValue().divide(java.math.BigDecimal.valueOf(c), 2, java.math.RoundingMode.HALF_UP).toPlainString());
        }

        String bodyMd = task.getReportContent() == null ? "" : task.getReportContent();
        String upper  = format == null ? "HTML" : format.toUpperCase();
        String fileName = "evaluation-" + id + "." + lowerExt(upper);

        byte[] bytes;
        MediaType mediaType;
        switch (upper) {
            case "MARKDOWN" -> {
                bytes = reportBuilder.buildEvaluationMarkdown(id, regulationName, periodStart, periodEnd,
                        overall, dimScores, bodyMd).getBytes(StandardCharsets.UTF_8);
                mediaType = MediaType.parseMediaType("text/markdown;charset=utf-8");
            }
            case "DOCX" -> {
                bytes = reportBuilder.buildEvaluationDocx(id, regulationName, periodLabel,
                        overall, dimScores, bodyMd);
                mediaType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }
            default -> {
                bytes = reportBuilder.wrapHtml(regulationName + " · 实施评估报告", bodyMd)
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

    private static String translateDim(String d) {
        if (d == null) return "未分类";
        return switch (d) {
            case EvaluationIndicator.DIM_LEGALITY      -> "合法性";
            case EvaluationIndicator.DIM_EXECUTION     -> "落实性";
            case EvaluationIndicator.DIM_SATISFACTION  -> "满意度";
            default -> d;
        };
    }
    private static String lowerExt(String format) {
        return switch (format == null ? "" : format.toUpperCase()) {
            case "MARKDOWN" -> "md";
            case "DOCX"     -> "docx";
            default         -> "html";
        };
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