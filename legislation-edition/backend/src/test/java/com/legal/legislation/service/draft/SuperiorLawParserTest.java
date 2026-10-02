package com.legal.legislation.service.draft;

import com.legal.legislation.service.draft.SuperiorLawParser.ParsedItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SuperiorLawParser 单元测试(纯规则,不需要 Spring context)。
 */
class SuperiorLawParserTest {

    private final SuperiorLawParser parser = new SuperiorLawParser();

    @Test
    void parse_blankText_returnsEmpty() {
        assertTrue(parser.parse(null, "X").isEmpty());
        assertTrue(parser.parse("",   "X").isEmpty());
        assertTrue(parser.parse("   ", "X").isEmpty());
    }

    @Test
    void parse_splitsArticles_andExtractsAuthorization() {
        String text = """
            第一条 为了规范 XX 活动,制定本法。
            第二条 具体办法由国务院另行规定。
            第三条 国家鼓励和支持 XX 发展。
            """;
        List<ParsedItem> items = parser.parse(text, "测试法");
        assertEquals(3, items.size(), "应识别 3 条");

        // 第一条: 含"应当"(deliberative)但没有授权词
        assertEquals("第一条", items.get(0).articleNo);
        assertEquals("DELIBERATIVE", items.get(0).type);

        // 第二条: 授权词"另行规定"
        ParsedItem auth = items.stream()
            .filter(i -> "第二条".equals(i.articleNo))
            .findFirst().orElseThrow();
        assertEquals("AUTHORIZATION", auth.type);
        assertTrue(auth.reason.contains("另行规定"));
    }

    @Test
    void parse_longArticle_doesNotTriggerDeliberative() {
        // 长条款(> 200 字)即使含 deliberative 动词也不入条
        String longBody = "应当".repeat(120); // 240 字
        String text = "第一条 " + longBody;
        List<ParsedItem> items = parser.parse(text, "X");
        assertTrue(items.isEmpty(), "超长 deliberative 条款应跳过,实际=" + items.size());
    }

    @Test
    void parse_multipleChapters_handlesCorrectly() {
        String text = """
            第一章 总则
            第一条 为规范 X 制定本法。
            第二章 监督管理
            第二条 国务院有关部门应当加强监督管理。
            """;
        List<ParsedItem> items = parser.parse(text, "X");
        // 第二条包含"应当",且 <200 字 → 应入 1 条 deliberative
        assertTrue(items.stream().anyMatch(i -> "第二条".equals(i.articleNo) && "DELIBERATIVE".equals(i.type)));
    }
}
