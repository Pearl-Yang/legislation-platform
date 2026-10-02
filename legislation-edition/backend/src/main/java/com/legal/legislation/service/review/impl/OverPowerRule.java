package com.legal.legislation.service.review.impl;

import com.legal.legislation.service.review.ReviewRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则 2:越权立法(RED)
 *
 * 行政立法只能规定上位法授权范围内的事项;超出 = 越权。
 * 简化策略:扫描草案内"设定 / 审批 / 收费 / 处罚"等敏感动词,标记需要复核
 */
@Component
public class OverPowerRule implements ReviewRule {

    @Override public String code()     { return "OVER_POWER"; }
    @Override public String name()     { return "越权立法"; }
    @Override public String severity() { return "RED"; }
    @Override public String type()     { return "POWER"; }

    private static final List<String> SENSITIVE_VERBS = List.of(
        "设定行政许可", "设定行政处罚", "设定行政强制", "设定行政收费",
        "设定审批", "增设行政许可", "增设行政处罚",
        "限制人身自由", "设定刑事责任"
    );

    @Override
    public List<MatchResult> apply(RuleContext ctx) {
        List<MatchResult> hits = new ArrayList<>();
        if (ctx.draftContent() == null) return hits;
        for (String v : SENSITIVE_VERBS) {
            if (ctx.draftContent().contains(v)) {
                hits.add(new MatchResult(
                    "OVER_POWER",
                    "RED",
                    "全文",
                    "草案含「" + v + "」表述,需确认是否在上位法授权范围内",
                    "请核对上位法是否明确授权;若未授权,删除或改写为程序性表述",
                    null
                ));
            }
        }
        return hits;
    }
}
