package com.legal.legislation.service.review.impl;

import com.legal.legislation.service.review.ReviewRule;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 规则 4:条文重复(YELLOW)
 *
 * 简化策略:用 n-gram 相似度检查草案与本地规则库的重复度
 *   - n=3(三元组)分词,统计共享 3-gram 数 / max(总 3-gram 数)
 *   - 相似度 > 0.6 = 重复嫌疑
 */
@Component
public class DuplicateRule implements ReviewRule {

    @Override public String code()     { return "DUPLICATE"; }
    @Override public String name()     { return "条文重复"; }
    @Override public String severity() { return "YELLOW"; }
    @Override public String type()     { return "DUPLICATE"; }

    private static final int N = 3;
    private static final double THRESHOLD = 0.6;

    @Override
    public List<MatchResult> apply(RuleContext ctx) {
        List<MatchResult> hits = new ArrayList<>();
        if (ctx.draftContent() == null || ctx.localRules() == null) return hits;
        Set<String> draftGrams = ngrams(ctx.draftContent(), N);

        for (var entry : ctx.localRules().entrySet()) {
            String regId   = String.valueOf(entry.getKey());
            String regText = String.valueOf(entry.getValue());
            Set<String> regGrams = ngrams(regText, N);
            if (regGrams.isEmpty()) continue;
            double sim = jaccard(draftGrams, regGrams);
            if (sim >= THRESHOLD) {
                hits.add(new MatchResult(
                    "DUPLICATE",
                    "YELLOW",
                    "全文",
                    String.format("与法规 #%s 相似度 %.0f%%,疑似重复", regId, sim * 100),
                    "请评估是否需要合并 / 引用;若为合理重复,请在起草说明中注明",
                    Long.valueOf(regId)
                ));
            }
        }
        return hits;
    }

    private static Set<String> ngrams(String text, int n) {
        if (text == null || text.length() < n) return Set.of();
        // 过滤空白
        String s = text.replaceAll("\\s+", "");
        Set<String> set = new HashSet<>();
        for (int i = 0; i <= s.length() - n; i++) {
            set.add(s.substring(i, i + n));
        }
        return set;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / union.size();
    }
}
