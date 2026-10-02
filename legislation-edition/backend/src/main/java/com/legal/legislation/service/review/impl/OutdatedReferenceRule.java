package com.legal.legislation.service.review.impl;

import com.legal.legislation.service.review.ReviewRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 规则 3:引用失效法条(YELLOW)
 *
 * 简化策略:扫描"依据《XXX》"等引用,看 regulation 表中对应法规 status 是否为 EXPIRED/ABOLISHED
 * Phase 1 不查全量知识图谱,只查 MySQL 的 regulation.status
 */
@Component
public class OutdatedReferenceRule implements ReviewRule {

    @Override public String code()     { return "OUTDATED_REF"; }
    @Override public String name()     { return "引用失效法条"; }
    @Override public String severity() { return "YELLOW"; }
    @Override public String type()     { return "OUTDATED"; }

    private static final Pattern CITE_PAT = Pattern.compile("[《\"]([^》\"]+)[》\"]");

    /** 调用方需要在 context.superiorLaws() 中预置 name->status 映射 */
    @Override
    public List<MatchResult> apply(RuleContext ctx) {
        List<MatchResult> hits = new ArrayList<>();
        if (ctx.draftContent() == null) return hits;
        if (ctx.superiorLaws() == null)  return hits;
        // superiorLaws 在此场景下当作 name -> status 字典
        @SuppressWarnings("unchecked")
        Map<String, String> statusMap = (Map<String, String>) (Map<?, ?>) ctx.superiorLaws();

        Matcher m = CITE_PAT.matcher(ctx.draftContent());
        while (m.find()) {
            String cited = m.group(1).trim();
            for (var e : statusMap.entrySet()) {
                if (e.getKey() != null && cited.contains(e.getKey())) {
                    String st = e.getValue();
                    if ("EXPIRED".equalsIgnoreCase(st) || "ABOLISHED".equalsIgnoreCase(st)) {
                        hits.add(new MatchResult(
                            "OUTDATED_REF",
                            "YELLOW",
                            "全文",
                            "引用了「" + cited + "」,该法规当前状态为 " + st,
                            "请核对引用条款,改用现行有效版本",
                            null
                        ));
                    }
                    break;
                }
            }
        }
        return hits;
    }
}
