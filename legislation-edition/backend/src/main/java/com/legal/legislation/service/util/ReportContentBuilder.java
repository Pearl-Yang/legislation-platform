package com.legal.legislation.service.util;

import com.legal.legislation.entity.DraftVersionHistory;
import com.legal.legislation.entity.LegislativeDraft;
import com.legal.legislation.entity.LegislativeProject;
import com.legal.legislation.mapper.LegislativeProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告正文公共构建器：把 draft / evaluation / consultation / cleanup 4 类报告
 * 都归一化成 Markdown 文本或 POI 章节序列，避免每个 Service 重复造轮子。
 *
 * 输出策略：
 *   - Markdown  ：在正文前加 YAML Front Matter（项目名/版本/起草人/导出时间等元信息）
 *   - DOCX      ：用 DocxExporter 的 h1/h2/h3/p/kv/table 五种 section 排版
 *   - HTML      ：由 controller 拼极简 HTML 壳（暂不细化）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportContentBuilder {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired @Lazy
    private LegislativeProjectMapper projectMapper;
    @Autowired @Lazy
    private DocxExporter docxExporter;

    // =========================== 立法草案 ===========================

    /**
     * 草案的 Markdown 正文（带 YAML Front Matter 元信息头）。
     */
    public String buildDraftMarkdown(LegislativeDraft draft) {
        StringBuilder sb = new StringBuilder();
        appendYamlHeader(sb, "立法草案",
                Map.of(
                    "draftId",        String.valueOf(draft.getId()),
                    "version",        String.valueOf(draft.getVersion()),
                    "projectId",      String.valueOf(draft.getProjectId()),
                    "projectName",    safeProjectName(draft.getProjectId()),
                    "generationType", nullSafe(draft.getGenerationType()),
                    "createdBy",      String.valueOf(draft.getCreatedBy()),
                    "createdAt",      draft.getCreatedAt() == null ? "" : draft.getCreatedAt().format(TS),
                    "exportedAt",     LocalDateTime.now().format(TS)
                ));
        if (draft.getDraftContent() != null) {
            sb.append(draft.getDraftContent());
        }
        return sb.toString();
    }

    /**
     * 草案的 DOCX 章节序列（封面 + 元信息 KV 表 + 条款正文）。
     */
    public List<Map<String, Object>> buildDraftSections(LegislativeDraft draft) {
        List<Map<String, Object>> sections = new ArrayList<>();
        // 封面
        sections.add(section("h1", safeProjectName(draft.getProjectId()) + " 草案（第 " + draft.getVersion() + " 版）"));
        // 元信息
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("草案 ID",       draft.getId());
        meta.put("版本号",        "第 " + draft.getVersion() + " 版");
        meta.put("所属项目",      safeProjectName(draft.getProjectId()));
        meta.put("生成方式",      translateGenType(draft.getGenerationType()));
        meta.put("创建人 ID",     draft.getCreatedBy());
        meta.put("创建时间",      draft.getCreatedAt() == null ? "" : draft.getCreatedAt().format(TS));
        meta.put("导出时间",      LocalDateTime.now().format(TS));
        sections.add(section("kv", meta));
        // 正文
        String body = draft.getDraftContent();
        if (body != null && !body.isBlank()) {
            for (String block : body.split("(?m)(?=^#{1,3} )")) {
                String trimmed = block.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("### "))      sections.add(section("h3", trimmed.substring(4).trim()));
                else if (trimmed.startsWith("## "))  sections.add(section("h2", trimmed.substring(3).trim()));
                else if (trimmed.startsWith("# "))   sections.add(section("h1", trimmed.substring(2).trim()));
                else                                  sections.add(section("p",  trimmed));
            }
        }
        return sections;
    }

    /**
     * 草案 DOCX 二进制流。
     */
    public byte[] buildDraftDocx(LegislativeDraft draft) {
        String title = safeProjectName(draft.getProjectId()) + " 草案（第 " + draft.getVersion() + " 版）";
        return docxExporter.export(title, buildDraftSections(draft));
    }

    // =========================== 评估报告 ===========================

    public String buildEvaluationMarkdown(Long taskId, String regulationName,
                                          String periodStart, String periodEnd,
                                          String overallScore, Map<String, String> dimensionScores,
                                          String bodyMarkdown) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("taskId",       String.valueOf(taskId));
        meta.put("regulation",   nullSafe(regulationName));
        meta.put("periodStart",  nullSafe(periodStart));
        meta.put("periodEnd",    nullSafe(periodEnd));
        meta.put("overallScore", nullSafe(overallScore));
        if (dimensionScores != null && !dimensionScores.isEmpty()) {
            for (Map.Entry<String, String> e : dimensionScores.entrySet()) {
                meta.put("dim_" + e.getKey(), e.getValue());
            }
        }
        meta.put("exportedAt", LocalDateTime.now().format(TS));
        appendYamlHeader(sb, "法规实施评估报告", meta);
        if (bodyMarkdown != null && !bodyMarkdown.isBlank()) sb.append(bodyMarkdown);
        return sb.toString();
    }

    public byte[] buildEvaluationDocx(Long taskId, String regulationName, String periodLabel,
                                      String overallScore, Map<String, String> dimensionScores,
                                      String bodyMarkdown) {
        String title = nullSafe(regulationName) + " · 实施评估报告";
        List<Map<String, Object>> sections = new ArrayList<>();
        sections.add(section("h1", title));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("任务 ID",     taskId);
        meta.put("评估周期",    nullSafe(periodLabel));
        meta.put("综合得分",    nullSafe(overallScore));
        if (dimensionScores != null) {
            for (Map.Entry<String, String> e : dimensionScores.entrySet()) {
                meta.put(e.getKey(), e.getValue());
            }
        }
        meta.put("导出时间",    LocalDateTime.now().format(TS));
        sections.add(section("kv", meta));
        if (bodyMarkdown != null) {
            appendMarkdownAsSections(sections, bodyMarkdown);
        }
        return docxExporter.export(title, sections);
    }

    // =========================== 咨询报告 ===========================

    public String buildConsultationMarkdown(Long consultationId, String title, String status,
                                            Integer totalViews, Integer totalOpinions,
                                            Map<String, Map<String, Integer>> byCategory,
                                            String bodyMarkdown) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("consultationId", String.valueOf(consultationId));
        meta.put("title",          nullSafe(title));
        meta.put("status",         nullSafe(status));
        meta.put("totalViews",     totalViews == null ? "0" : totalViews.toString());
        meta.put("totalOpinions",  totalOpinions == null ? "0" : totalOpinions.toString());
        if (byCategory != null) {
            int idx = 1;
            for (Map.Entry<String, Map<String, Integer>> e : byCategory.entrySet()) {
                Map<String, Integer> b = e.getValue();
                meta.put("cat_" + idx + "_name",     e.getKey());
                meta.put("cat_" + idx + "_total",    String.valueOf(b.getOrDefault("total", 0)));
                meta.put("cat_" + idx + "_support",  String.valueOf(b.getOrDefault("support", 0)));
                meta.put("cat_" + idx + "_oppose",   String.valueOf(b.getOrDefault("oppose", 0)));
                meta.put("cat_" + idx + "_neutral",  String.valueOf(b.getOrDefault("neutral", 0)));
                idx++;
            }
        }
        meta.put("exportedAt", LocalDateTime.now().format(TS));
        appendYamlHeader(sb, "公众意见征集报告", meta);
        if (bodyMarkdown != null && !bodyMarkdown.isBlank()) sb.append(bodyMarkdown);
        return sb.toString();
    }

    public byte[] buildConsultationDocx(Long consultationId, String title, String status,
                                        Integer totalViews, Integer totalOpinions,
                                        Map<String, Map<String, Integer>> byCategory,
                                        String bodyMarkdown) {
        String docTitle = nullSafe(title) + " · 意见征集报告";
        List<Map<String, Object>> sections = new ArrayList<>();
        sections.add(section("h1", docTitle));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("征集 ID",      consultationId);
        meta.put("状态",         nullSafe(status));
        meta.put("访问量",       totalViews == null ? 0 : totalViews);
        meta.put("意见总数",     totalOpinions == null ? 0 : totalOpinions);
        meta.put("导出时间",     LocalDateTime.now().format(TS));
        sections.add(section("kv", meta));
        if (byCategory != null && !byCategory.isEmpty()) {
            sections.add(section("h2", "意见分类统计"));
            List<String> headers = List.of("类别", "总数", "支持", "反对", "中立");
            List<List<String>> rows = new ArrayList<>();
            for (Map.Entry<String, Map<String, Integer>> e : byCategory.entrySet()) {
                Map<String, Integer> b = e.getValue();
                rows.add(List.of(
                        e.getKey(),
                        String.valueOf(b.getOrDefault("total", 0)),
                        String.valueOf(b.getOrDefault("support", 0)),
                        String.valueOf(b.getOrDefault("oppose", 0)),
                        String.valueOf(b.getOrDefault("neutral", 0))
                ));
            }
            sections.add(section("table", Map.of("headers", headers, "rows", rows)));
        }
        if (bodyMarkdown != null) {
            appendMarkdownAsSections(sections, bodyMarkdown);
        }
        return docxExporter.export(docTitle, sections);
    }

    // =========================== 清理报告 ===========================

    public String buildCleanupMarkdown(Long taskId, String taskName, String taskType, String status,
                                       Integer total, Integer keep, Integer modify, Integer obsolete,
                                       String bodyMarkdown) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("taskId",   String.valueOf(taskId));
        meta.put("taskName", nullSafe(taskName));
        meta.put("taskType", nullSafe(taskType));
        meta.put("status",   nullSafe(status));
        meta.put("total",    total == null ? "0" : total.toString());
        meta.put("keep",     keep == null ? "0" : keep.toString());
        meta.put("modify",   modify == null ? "0" : modify.toString());
        meta.put("obsolete", obsolete == null ? "0" : obsolete.toString());
        meta.put("exportedAt", LocalDateTime.now().format(TS));
        appendYamlHeader(sb, "法规清理任务报告", meta);
        if (bodyMarkdown != null && !bodyMarkdown.isBlank()) sb.append(bodyMarkdown);
        return sb.toString();
    }

    public byte[] buildCleanupDocx(Long taskId, String taskName, String taskType, String status,
                                   Integer total, Integer keep, Integer modify, Integer obsolete,
                                   String bodyMarkdown) {
        String docTitle = nullSafe(taskName) + " · 清理任务报告";
        List<Map<String, Object>> sections = new ArrayList<>();
        sections.add(section("h1", docTitle));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("任务 ID",     taskId);
        meta.put("触发模式",    translateTaskType(taskType));
        meta.put("状态",        nullSafe(status));
        meta.put("候选法规数",  total == null ? 0 : total);
        meta.put("保留",        keep == null ? 0 : keep);
        meta.put("修订",        modify == null ? 0 : modify);
        meta.put("废止",        obsolete == null ? 0 : obsolete);
        meta.put("导出时间",    LocalDateTime.now().format(TS));
        sections.add(section("kv", meta));
        if (bodyMarkdown != null) {
            appendMarkdownAsSections(sections, bodyMarkdown);
        }
        return docxExporter.export(docTitle, sections);
    }

    // =========================== 通用 HTML 包装 ===========================

    public String wrapHtml(String title, String bodyMarkdown) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">");
        sb.append("<title>").append(escapeHtml(title)).append("</title>");
        sb.append("<style>");
        sb.append("body{font-family:'PingFang SC','Microsoft YaHei',sans-serif;max-width:880px;margin:32px auto;padding:0 24px;line-height:1.8;color:#222}");
        sb.append("h1{text-align:center;font-size:22px;margin-bottom:8px}");
        sb.append("h2{font-size:18px;border-bottom:1px solid #eee;padding-bottom:4px;margin-top:24px}");
        sb.append("h3{font-size:15px;margin-top:18px}");
        sb.append("pre,code{background:#f6f8fa;padding:2px 6px;border-radius:4px;font-family:Consolas,monospace}");
        sb.append("table{border-collapse:collapse;width:100%;margin:12px 0}");
        sb.append("th,td{border:1px solid #dcdfe6;padding:6px 10px;font-size:13px}");
        sb.append("th{background:#fafbfc}");
        sb.append(".meta{background:#fafbfc;border:1px solid #ebeef5;border-radius:6px;padding:12px 16px;font-size:13px;color:#606266}");
        sb.append("</style></head><body>");
        sb.append("<h1>").append(escapeHtml(title)).append("</h1>");
        sb.append("<div class=\"meta\">导出时间：").append(LocalDateTime.now().format(TS)).append("</div>");
        if (bodyMarkdown != null) {
            sb.append(renderMarkdownLight(bodyMarkdown));
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    // =========================== 工具 ===========================

    private Map<String, Object> section(String type, Object content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        if ("kv".equals(type) || "table".equals(type)) {
            m.putAll((Map) content);
        } else if ("h1".equals(type) || "h2".equals(type) || "h3".equals(type) || "p".equals(type)) {
            m.put("text", content);
        } else if ("blank".equals(type)) {
            // nothing
        }
        return m;
    }

    private void appendYamlHeader(StringBuilder sb, String title, Map<String, Object> meta) {
        sb.append("---\n");
        sb.append("title: ").append(yamlEscape(title)).append("\n");
        if (meta != null) {
            for (Map.Entry<String, Object> e : meta.entrySet()) {
                if (e.getValue() == null) continue;
                sb.append(e.getKey()).append(": ").append(yamlEscape(String.valueOf(e.getValue()))).append("\n");
            }
        }
        sb.append("---\n\n");
    }

    private void appendMarkdownAsSections(List<Map<String, Object>> sections, String md) {
        for (String block : md.split("(?m)(?=^#{1,3} )")) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("### "))      sections.add(section("h3", trimmed.substring(4).trim()));
            else if (trimmed.startsWith("## "))  sections.add(section("h2", trimmed.substring(3).trim()));
            else if (trimmed.startsWith("# "))   sections.add(section("h1", trimmed.substring(2).trim()));
            else                                  sections.add(section("p",  trimmed));
        }
    }

    private String safeProjectName(Long projectId) {
        if (projectId == null) return "(未关联项目)";
        try {
            LegislativeProject p = projectMapper.selectById(projectId);
            if (p == null) return "项目#" + projectId;
            return nullSafe(p.getProjectName());
        } catch (Exception ex) {
            log.warn("查询项目名失败 id={}: {}", projectId, ex.getMessage());
            return "项目#" + projectId;
        }
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static String nullSafe(Object o) { return o == null ? "" : o.toString(); }

    private static String translateGenType(String t) {
        if (t == null) return "未知";
        return switch (t) {
            case LegislativeDraft.GEN_TYPE_AUTO   -> "AI 自动生成";
            case LegislativeDraft.GEN_TYPE_MANUAL -> "人工起草";
            default -> t;
        };
    }

    private static String translateTaskType(String t) {
        if (t == null) return "未知";
        return switch (t) {
            case "DAILY"    -> "日常清理";
            case "PERIODIC" -> "定期清理";
            case "THEMATIC" -> "专项清理";
            default -> t;
        };
    }

    private static String yamlEscape(String s) {
        if (s == null) return "\"\"";
        if (s.contains(":") || s.contains("#") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return s;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String renderMarkdownLight(String md) {
        if (md == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : md.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) { sb.append("<br/>\n"); continue; }
            if (t.startsWith("### "))      sb.append("<h3>").append(escapeHtml(t.substring(4))).append("</h3>\n");
            else if (t.startsWith("## "))  sb.append("<h2>").append(escapeHtml(t.substring(3))).append("</h2>\n");
            else if (t.startsWith("# "))   sb.append("<h1>").append(escapeHtml(t.substring(2))).append("</h1>\n");
            else if (t.startsWith("|") && t.endsWith("|")) {
                // 简化：不解析 Markdown 表格，直接当段落
                sb.append("<p>").append(escapeHtml(t)).append("</p>\n");
            } else {
                sb.append("<p>").append(escapeHtml(t)).append("</p>\n");
            }
        }
        return sb.toString();
    }
}
