package com.legal.legislation.service.review.impl;

import com.legal.legislation.service.review.ReviewRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 规则 1:与上位法冲突(RED)
 *
 * 简化策略:从 draft 中抽出与上位法条款互斥 / 限缩 / 扩张的措辞
 * 真实实现:用 LLM 判定"实质性矛盾";此处用关键词命中 + 动词方向对比
 */
@Component
public class SuperiorConflictRule implements ReviewRule {

    @Override public String code()     { return "SUPERIOR_CONFLICT"; }
    @Override public String name()     { return "与上位法冲突"; }
    @Override public String severity() { return "RED"; }
    @Override public String type()     { return "CONFLICT"; }

    /** 同义改写后的反义动词(下位法用 A 动词 vs 上位法用 B 动词 = 冲突) */
    private static final Map<String, String> CONFLICT_VERBS = Map.of(
        "应当", "不得",
        "不得", "应当",
        "禁止", "允许",
        "允许", "禁止"
    );

    @Override
    public List<MatchResult> apply(RuleContext ctx) {
        List<MatchResult> hits = new ArrayList<>();
        if (ctx.draftContent() == null || ctx.superiorLaws() == null) return hits;

        for (var entry : ctx.superiorLaws().entrySet()) {
            String lawId   = String.valueOf(entry.getKey());
            String lawText = String.valueOf(entry.getValue());
            // 简版:扫描 draft 中"应当/不得"搭配,看是否与上位法相反
            for (var v : CONFLICT_VERBS.entrySet()) {
                if (ctx.draftContent().contains(v.getKey())
                    && !lawText.contains(v.getKey())
                    && lawText.contains(v.getValue())) {
                    hits.add(new MatchResult(
                        "CONFLICT_SUPERIOR",
                        "RED",
                        "全文",
                        "草案使用「" + v.getKey() + "」,而上位法对应位置为「" + v.getValue() + "」,疑似冲突",
                        "请对照上位法第 " + excerpt(lawText, v.getValue()) + " 条核对",
                        Long.valueOf(lawId)
                    ));
                    break; // 一条只报一次
                }
            }
        }
        return hits;
    }

    private String excerpt(String text, String kw) {
        int idx = text.indexOf(kw);
        if (idx < 0) return "前文";
        // 找最近的"第X条"标记
        int start = Math.max(0, idx - 50);
        int end   = Math.min(text.length(), idx + 50);
        String snip = text.substring(start, end).replaceAll("\\s+", " ");
        return "「" + snip + "…」附近";
    }
}
