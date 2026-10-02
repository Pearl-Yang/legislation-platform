package com.legal.legislation.service.review.impl;

import com.legal.legislation.service.review.ReviewRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则 5:格式不规范(BLUE)
 *
 * 检查项:
 *  - 条款编号不连续("第一条"后跳过"第三条"等)
 *  - 引用格式错误("第X条、第Y条"中间应为"、"连接)
 *  - 全角 / 半角混用
 */
@Component
public class FormatRule implements ReviewRule {

    @Override public String code()     { return "FORMAT"; }
    @Override public String name()     { return "格式不规范"; }
    @Override public String severity() { return "BLUE"; }
    @Override public String type()     { return "FORMAT"; }

    private static final Pattern ARTICLE_NO = Pattern.compile("第([一二三四五六七八九十百千零〇0-9]+)条");

    @Override
    public List<MatchResult> apply(RuleContext ctx) {
        List<MatchResult> hits = new ArrayList<>();
        if (ctx.draftContent() == null) return hits;
        String text = ctx.draftContent();

        // 1) 条款编号不连续
        Matcher m = ARTICLE_NO.matcher(text);
        int last = -1;
        boolean firstFound = false;
        while (m.find()) {
            int cur = parseCn(m.group(1));
            if (!firstFound) { firstFound = true; last = cur; continue; }
            if (cur != last + 1) {
                hits.add(new MatchResult(
                    "FORMAT_NUMBERING",
                    "BLUE",
                    m.group(0),
                    "条款编号不连续:从第" + cn(last) + "条跳到第" + cn(cur) + "条",
                    "请补充缺失的条款编号,或确认是否合并 / 删除",
                    null
                ));
            }
            last = cur;
        }

        // 2) 全角半角混用(简单检测:出现半角"," 在中文条款号列表中)
        if (text.matches("(?s).*第[一二三四五六七八九十]+条,第[一二三四五六七八九十]+条.*")) {
            hits.add(new MatchResult(
                "FORMAT_PUNCTUATION",
                "BLUE",
                "引用",
                "引用条款列表用了半角逗号「,」,应为顿号「、」",
                "将「第X条,第Y条」改为「第X条、第Y条」",
                null
            ));
        }

        return hits;
    }

    private static int parseCn(String s) {
        try { return Integer.parseInt(s); } catch (Exception ignore) {}
        // 简版:中文数字转 int(一/二/三...十)
        int n = 0, cur = 0;
        for (char c : s.toCharArray()) {
            int v = switch (c) {
                case '一' -> 1; case '二' -> 2; case '三' -> 3; case '四' -> 4; case '五' -> 5;
                case '六' -> 6; case '七' -> 7; case '八' -> 8; case '九' -> 9;
                case '零', '〇' -> 0; case '十' -> 10; default -> -1;
            };
            if (v < 0) continue;
            if (v == 10) { n += (cur == 0 ? 10 : cur * 10); cur = 0; }
            else { cur = v; n += cur; cur = 0; }
        }
        return n == 0 ? -1 : n;
    }

    private static String cn(int n) {
        return switch (n) {
            case 1 -> "一"; case 2 -> "二"; case 3 -> "三"; case 4 -> "四"; case 5 -> "五";
            case 6 -> "六"; case 7 -> "七"; case 8 -> "八"; case 9 -> "九"; case 10 -> "十";
            default -> String.valueOf(n);
        };
    }
}
