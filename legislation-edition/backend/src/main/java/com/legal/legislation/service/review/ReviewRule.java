package com.legal.legislation.service.review;

import java.util.List;
import java.util.Map;

/**
 * 审查规则接口。
 *
 * 每条规则独立实现,执行时遍历;最后汇总成 ReviewIssue 列表。
 */
public interface ReviewRule {

    /** 规则编码(与 review_rule 表 rule_code 一致) */
    String code();

    /** 规则名称 */
    String name();

    /** 严重程度 RED / YELLOW / BLUE / GREY */
    String severity();

    /** 规则类型:CONFLICT / POWER / OUTDATED / DUPLICATE / FORMAT / VERBOSE */
    String type();

    /**
     * 执行规则。
     *
     * @param context 含 草案全文、对比法规库、审查上下文
     * @return 命中的问题列表(空列表表示无问题)
     */
    List<MatchResult> apply(RuleContext context);

    /** 规则执行结果(对应 review_issue 字段) */
    record MatchResult(
        String issueType,
        String severity,
        String articleIndex,
        String description,
        String suggestion,
        Long referenceRegulationId
    ) {}

    /** 规则上下文。 */
    record RuleContext(
        String draftContent,
        Map<String, Object> superiorLaws,   // regulation_id -> fullText
        Map<String, Object> localRules      // 异地参考 regulation_id -> fullText
    ) {}
}
