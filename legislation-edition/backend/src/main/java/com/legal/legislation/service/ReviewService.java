package com.legal.legislation.service;

import com.legal.legislation.entity.ReviewIssue;
import com.legal.legislation.entity.ReviewRecord;
import com.legal.legislation.entity.ReviewRule;

import java.util.List;
import java.util.Map;

public interface ReviewService {

    /**
     * 提交一次审查（异步），返回 recordId。
     */
    Task<Long> submitReview(Long draftId, String reviewType);

    /**
     * 查审查记录（含问题列表）。
     */
    Task<Map<String, Object>> getReviewRecord(Long recordId);

    /**
     * 列出所有规则（可按类型 / 严重级别过滤）。
     */
    Task<List<ReviewRule>> listRules(String ruleType, String severity);

    /**
     * 更新规则（启用/禁用、改严重级别）。
     */
    Task<Boolean> updateRule(Long id, ReviewRule rule);

    /**
     * 标记问题已解决。
     */
    Task<Boolean> resolveIssue(Long issueId);

    /**
     * 批量审查。
     */
    Task<List<Long>> batchSubmit(List<Long> draftIds);
}