package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.legal.legislation.entity.DraftVersionHistory;
import com.legal.legislation.entity.LegislativeDraft;
import com.legal.legislation.entity.LegislativeProject;
import com.legal.legislation.mapper.DraftVersionHistoryMapper;
import com.legal.legislation.mapper.LegislativeDraftMapper;
import com.legal.legislation.mapper.LegislativeProjectMapper;
import com.legal.legislation.notify.NotifyMessage;
import com.legal.legislation.notify.NotifyService;
import com.legal.legislation.service.DraftService;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 草案生成 Service
 *
 * 当前 AI 接入（ai.draft-generation.enabled）未开启时，使用"模板占位"输出：
 *   - 第一章 总则
 *   - 第二章  ...
 *   - 等
 * 仅用于开发期跑通业务链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftServiceImpl implements DraftService {

    private final LegislativeDraftMapper      draftMapper;
    private final DraftVersionHistoryMapper  historyMapper;
    private final LegislativeProjectMapper   projectMapper;
    private final NotifyService              notifyService;

    @Value("${ai.draft-generation.enabled:false}")
    private boolean aiEnabled;

    /** 任务进度：taskId → {progress, status, draftId, error} */
    private final Map<String, Map<String, Object>> taskStore = new ConcurrentHashMap<>();

    @Override
    public Task<String> submitGenerateTask(Long projectId, Long stageId, String prompt, Long createdBy) {
        LegislativeProject project = projectMapper.selectById(projectId);
        if (project == null) {
            return Task.error("项目不存在: " + projectId);
        }

        // 找出当前最大 version
        QueryWrapper<LegislativeDraft> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId).orderByDesc("version").last("LIMIT 1");
        LegislativeDraft latest = draftMapper.selectOne(qw);
        int nextVersion = (latest == null) ? 1 : (latest.getVersion() + 1);

        // 立即返回 taskId
        String taskId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> taskInfo = new HashMap<>();
        taskInfo.put("taskId",     taskId);
        taskInfo.put("status",     "PROCESSING");
        taskInfo.put("progress",   0);
        taskInfo.put("projectId",  projectId);
        taskInfo.put("stageId",    stageId);
        taskInfo.put("prompt",     prompt);
        taskInfo.put("version",    nextVersion);
        taskInfo.put("createdBy",  createdBy);
        taskInfo.put("createdAt",  LocalDateTime.now());
        taskStore.put(taskId, taskInfo);

        // 异步执行生成
        runGenerateTaskAsync(taskId);

        return Task.ok(taskId);
    }

    @Async
    void runGenerateTaskAsync(String taskId) {
        try {
            Map<String, Object> info = taskStore.get(taskId);
            if (info == null) return;

            info.put("progress", 30);
            // 模拟 AI 生成耗时
            Thread.sleep(800);

            info.put("progress", 70);
            Thread.sleep(600);

            LegislativeDraft draft = renderDraft(info);
            draftMapper.insert(draft);

            // 写历史
            DraftVersionHistory hist = new DraftVersionHistory();
            hist.setDraftId(draft.getId());
            hist.setVersion(draft.getVersion());
            hist.setContentSnapshot(draft.getDraftContent());
            hist.setChangedBy(draft.getCreatedBy());
            hist.setChangedAt(LocalDateTime.now());
            hist.setChangeSummary("AI 自动生成 / 模板占位");
            historyMapper.insert(hist);

            info.put("progress", 100);
            info.put("status",   "SUCCESS");
            info.put("draftId",  draft.getId());

            // 异步通知
            notifyService.send(new NotifyMessage(
                draft.getCreatedBy(),
                "SYSTEM",
                "草案生成完成",
                String.format("项目 #%d 的第 %d 版草案已生成完毕。", draft.getProjectId(), draft.getVersion()),
                "legislative_draft",
                draft.getId(),
                LocalDateTime.now()
            ));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            Map<String, Object> info = taskStore.get(taskId);
            if (info != null) {
                info.put("status", "FAILED");
                info.put("error",  "任务被中断");
            }
        } catch (Exception ex) {
            log.error("生成草案任务失败 taskId={}", taskId, ex);
            Map<String, Object> info = taskStore.get(taskId);
            if (info != null) {
                info.put("status", "FAILED");
                info.put("error",  ex.getMessage());
            }
        }
    }

    /**
     * 实际生成内容。当前未开 AI 时，输出模板占位文本。
     */
    private LegislativeDraft renderDraft(Map<String, Object> info) {
        Long projectId   = ((Number) info.get("projectId")).longValue();
        Long stageId     = info.get("stageId") == null ? null : ((Number) info.get("stageId")).longValue();
        int  version     = ((Number) info.get("version")).intValue();
        Long createdBy   = info.get("createdBy") == null ? null : ((Number) info.get("createdBy")).longValue();
        String prompt    = (String) info.get("prompt");

        LegislativeDraft d = new LegislativeDraft();
        d.setProjectId(projectId);
        d.setStageId(stageId);
        d.setVersion(version);
        d.setGenerationType(aiEnabled ? LegislativeDraft.GEN_TYPE_AUTO : LegislativeDraft.GEN_TYPE_MANUAL);
        d.setPromptSnapshot(prompt);
        d.setCreatedBy(createdBy);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        d.setDraftContent(buildPlaceholderContent(projectId, version, prompt));
        return d;
    }

    private String buildPlaceholderContent(Long projectId, int version, String prompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 项目 ").append(projectId).append(" 草案 第").append(version).append("版\n\n");
        sb.append("> 生成方式: ").append(aiEnabled ? "AI 自动生成" : "模板占位（开发期）").append("\n");
        if (prompt != null && !prompt.isBlank()) {
            sb.append("> 提示词: ").append(prompt).append("\n");
        }
        sb.append("\n## 第一章 总则\n\n");
        sb.append("**第一条** (立法目的)\n为了 ...");
        sb.append("\n\n**第二条** (适用范围)\n本规定适用于 ...");
        sb.append("\n\n## 第二章 主体责任\n\n");
        sb.append("**第三条** ...\n");
        sb.append("\n## 第三章 监督管理\n\n");
        sb.append("**第四条** ...\n");
        sb.append("\n## 第四章 法律责任\n\n");
        sb.append("**第五条** ...\n");
        sb.append("\n## 第五章 附则\n\n");
        sb.append("**第六条** 本规定自 XXXX 年 XX 月 XX 日起施行。\n");
        return sb.toString();
    }

    @Override
    public Task<Map<String, Object>> getTaskStatus(String taskId) {
        Map<String, Object> info = taskStore.get(taskId);
        if (info == null) {
            return Task.error("任务不存在或已过期");
        }
        Map<String, Object> result = new HashMap<>(info);
        // 不向外暴露内部 prompt
        result.remove("prompt");
        return Task.ok(result);
    }

    @Override
    public Task<List<LegislativeDraft>> listByProject(Long projectId) {
        QueryWrapper<LegislativeDraft> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId).orderByDesc("version");
        return Task.ok(draftMapper.selectList(qw));
    }

    @Override
    public Task<LegislativeDraft> getDetail(Long id) {
        LegislativeDraft d = draftMapper.selectById(id);
        if (d == null) {
            return Task.error("草案不存在");
        }
        return Task.ok(d);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<LegislativeDraft> revise(Long id, String newContent, String changeSummary, Long changedBy) {
        LegislativeDraft exist = draftMapper.selectById(id);
        if (exist == null) {
            return Task.error("草案不存在");
        }
        if (newContent == null || newContent.isBlank()) {
            return Task.error("修订内容不能为空");
        }

        // 写当前快照到历史
        DraftVersionHistory hist = new DraftVersionHistory();
        hist.setDraftId(id);
        hist.setVersion(exist.getVersion());
        hist.setContentSnapshot(exist.getDraftContent());
        hist.setChangedBy(changedBy);
        hist.setChangedAt(LocalDateTime.now());
        hist.setChangeSummary(changeSummary == null ? "人工修订" : changeSummary);
        historyMapper.insert(hist);

        // 升 version 并写入新内容
        exist.setVersion(exist.getVersion() + 1);
        exist.setDraftContent(newContent);
        exist.setGenerationType(LegislativeDraft.GEN_TYPE_MANUAL);
        exist.setUpdatedAt(LocalDateTime.now());
        draftMapper.updateById(exist);

        return Task.ok(exist);
    }

    @Override
    public Task<List<DraftVersionHistory>> listVersions(Long id) {
        QueryWrapper<DraftVersionHistory> qw = new QueryWrapper<>();
        qw.eq("draft_id", id).orderByDesc("version");
        return Task.ok(historyMapper.selectList(qw));
    }

    @Override
    public Task<Map<String, Object>> export(Long id, String format) {
        LegislativeDraft d = draftMapper.selectById(id);
        if (d == null) {
            return Task.error("草案不存在");
        }
        String upper = format == null ? "MARKDOWN" : format.toUpperCase();
        Map<String, Object> result = new HashMap<>();
        result.put("draftId",     d.getId());
        result.put("format",      upper);
        result.put("fileName",    "draft-" + d.getId() + "-v" + d.getVersion() + "." + lowerExt(upper));
        result.put("contentType", contentType(upper));
        result.put("content",     d.getDraftContent());
        result.put("size",        d.getDraftContent() == null ? 0 : d.getDraftContent().getBytes(StandardCharsets.UTF_8).length);
        result.put("exportedAt",  LocalDateTime.now());
        return Task.ok(result);
    }

    private static String lowerExt(String format) {
        return switch (format) {
            case "DOCX" -> "docx";
            case "HTML" -> "html";
            default     -> "md";
        };
    }

    private static String contentType(String format) {
        return switch (format) {
            case "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "HTML" -> "text/html; charset=utf-8";
            default     -> "text/markdown; charset=utf-8";
        };
    }
}