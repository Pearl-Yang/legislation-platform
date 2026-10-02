package com.legal.legislation.service;

import com.legal.legislation.entity.DraftVersionHistory;
import com.legal.legislation.entity.LegislativeDraft;

import java.util.List;
import java.util.Map;

public interface DraftService {

    /**
     * 异步提交生成任务，立即返回 taskId。
     */
    Task<String> submitGenerateTask(Long projectId, Long stageId, String prompt, Long createdBy);

    /**
     * 轮询任务状态。PROCESSING / SUCCESS / FAILED。
     */
    Task<Map<String, Object>> getTaskStatus(String taskId);

    /**
     * 查询某项目下的所有草案，按 version 倒序。
     */
    Task<List<LegislativeDraft>> listByProject(Long projectId);

    /**
     * 查单个草案详情。
     */
    Task<LegislativeDraft> getDetail(Long id);

    /**
     * 提交人工修订，写入历史并升 version。
     */
    Task<LegislativeDraft> revise(Long id, String newContent, String changeSummary, Long changedBy);

    /**
     * 查版本历史。
     */
    Task<List<DraftVersionHistory>> listVersions(Long id);

    /**
     * 导出草案（MARKDOWN / HTML / DOCX）。
     */
    Task<Map<String, Object>> export(Long id, String format);
}