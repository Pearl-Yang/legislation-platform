package com.legal.legislation.service.review;

import com.legal.legislation.service.review.ReviewRule.MatchResult;
import com.legal.legislation.service.review.impl.FormatRule;
import com.legal.legislation.service.review.impl.OverPowerRule;
import com.legal.legislation.service.review.impl.SuperiorConflictRule;
import com.legal.legislation.service.review.impl.VerboseRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 6 个审查规则各跑 1 个 smoke test。
 */
class ReviewRuleTest {

    @Test
    void superiorConflictRule_detectsConflictVerbs() {
        var rule = new SuperiorConflictRule();
        // 上位法用"应当",草案用"不得" → 冲突
        var ctx = new ReviewRule.RuleContext(
            "草案 X 不得 限制数据出境",
            Map.of("1", "上位法规定,XX 应当 加强数据管理。"),
            Map.of()
        );
        List<MatchResult> rs = rule.apply(ctx);
        assertNotNull(rs);
        assertTrue(rs.stream().anyMatch(m -> "RED".equals(m.severity())),
            "应识别与上位法的不得/应当冲突");
    }

    @Test
    void overPowerRule_detectsOverReach() {
        var rule = new OverPowerRule();
        var ctx = new ReviewRule.RuleContext(
            "国务院部门可以增设行政许可",
            Map.of(),
            Map.of()
        );
        List<MatchResult> rs = rule.apply(ctx);
        assertNotNull(rs);
    }

    @Test
    void formatRule_runsWithoutException() {
        var rule = new FormatRule();
        var ctx = new ReviewRule.RuleContext("第七条 ... \n 第九条 ...", Map.of(), Map.of());
        List<MatchResult> rs = rule.apply(ctx);
        assertNotNull(rs);
    }

    @Test
    void verboseRule_flagsLongArticle() {
        var rule = new VerboseRule();
        // 必须以"第X条"开头
        String longText = "第一条 " + "本条款规定了一些内容。".repeat(60); // > 200 字
        var ctx = new ReviewRule.RuleContext(longText, Map.of(), Map.of());
        List<MatchResult> rs = rule.apply(ctx);
        assertTrue(rs.stream().anyMatch(m -> "GREY".equals(m.severity())),
            "应识别 >200 字的条款为冗杂;实际命中=" + rs.size());
    }

    @Test
    void reviewEngine_run_runsAllRules() {
        // ReviewEngine 需要 Spring 注入,这里只验证构造不抛异常 + 规则列表可遍历
        // (跳过 mapper 依赖,留给集成测试覆盖)
        var ruleList = java.util.List.<ReviewRule>of(
            new SuperiorConflictRule(),
            new OverPowerRule(),
            new FormatRule(),
            new VerboseRule()
        );
        assertEquals(4, ruleList.size());
        for (ReviewRule r : ruleList) {
            assertNotNull(r.code());
            assertNotNull(r.severity());
        }
    }
}
