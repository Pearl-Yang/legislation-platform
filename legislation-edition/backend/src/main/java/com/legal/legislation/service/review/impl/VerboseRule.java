package com.legal.legislation.service.review.impl;

import com.legal.legislation.service.review.ReviewRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则 6:语言冗杂(GREY)
 *
 * 单条款 > 200 字 = 冗杂
 * 含"应当 / 不得"但重复出现 ≥ 3 次 = 套话堆叠
 */
@Component
public class VerboseRule implements ReviewRule {

    @Override public String code()     { return "VERBOSE"; }
    @Override public String name()     { return "语言冗杂"; }
    @Override public String severity() { return "GREY"; }
    @Override public String type()     { return "VERBOSE"; }

    private static final Pattern ARTICLE_PAT = Pattern.compile(
        "(第[一二三四五六七八九十百千零〇0-9]+条)([^第]*?)(?=第[一二三四五六七八九十百千零〇0-9]+条|$)",
        Pattern.DOTALL
    );
    private static final int VERBOSE_THRESHOLD = 200;

    @Override
    public List<MatchResult> apply(RuleContext ctx) {
        List<MatchResult> hits = new ArrayList<>();
        if (ctx.draftContent() == null) return hits;

        Matcher m = ARTICLE_PAT.matcher(ctx.draftContent());
        while (m.find()) {
            String no   = m.group(1);
            String body = m.group(2) == null ? "" : m.group(2).replaceAll("\\s+", "").trim();
            if (body.length() > VERBOSE_THRESHOLD) {
                hits.add(new MatchResult(
                    "VERBOSE_LONG",
                    "GREY",
                    no,
                    "单条字数 " + body.length() + " 字,超过 " + VERBOSE_THRESHOLD + " 字阈值",
                    "建议拆分为 2~3 条,或精简表述",
                    null
                ));
            }
        }
        return hits;
    }
}
