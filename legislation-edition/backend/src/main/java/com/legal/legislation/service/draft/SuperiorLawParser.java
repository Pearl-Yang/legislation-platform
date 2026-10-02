package com.legal.legislation.service.draft;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 上位法条款解析器(规则引擎版,Phase 1)。
 *
 * 输入:上位法全文(纯文本)
 * 输出:可"细化"的事项清单
 *
 * 提取策略(由轻到重,3 路并行):
 *  1) 条款切片:按"第X条 / 第X章"切成条款级片段
 *  2) 关键词抽取:命中"应当 / 可以 / 不得 / 鼓励 / 支持"等含裁量空间动词的句子
 *  3) 授权条款:命中"由 XX 制定 / 另行规定 / 具体办法由 XX 规定"等
 *
 * 后续 LLM 接入:用 Qwen 替换关键词层,保留条款切片作为 prompt context
 */
@Slf4j
@Component
public class SuperiorLawParser {

    /** 条款编号正则:第 X 条 / 第 X 章 / 第 X 款 */
    private static final Pattern ARTICLE_PAT = Pattern.compile(
        "(第[一二三四五六七八九十百千零〇0-9]+条)([^第]*?)(?=第[一二三四五六七八九十百千零〇0-9]+条|$)",
        Pattern.DOTALL
    );
    private static final Pattern CHAPTER_PAT = Pattern.compile(
        "(第[一二三四五六七八九十百千零〇0-9]+章)([^第]*?)(?=第[一二三四五六七八九十百千零〇0-9]+章|$)",
        Pattern.DOTALL
    );

    /** 含裁量空间 / 待细化的动词 */
    private static final List<String> DELIBERATIVE_VERBS = Arrays.asList(
        "应当", "可以", "不得", "鼓励", "支持", "引导", "推动", "规范",
        "加强", "健全", "完善", "建立", "制定", "规定", "明确"
    );

    /** 授权条款关键词(通常意味着下位法需要细化) */
    private static final List<String> AUTHORIZATION_KEYWORDS = Arrays.asList(
        "另行制定", "另行规定", "具体办法", "具体措施", "具体标准",
        "由省、自治区、直辖市", "由国务院有关部门", "由县级以上",
        "具体范围", "具体条件", "具体程序", "具体要求", "配套规定"
    );

    /**
     * 解析上位法全文,返回可"细化"事项清单。
     *
     * @param fullText 上位法正文
     * @param sourceName 上位法名称(用于显示)
     * @return 事项列表,每条带 articleNo / articleText / reason / type
     */
    public List<ParsedItem> parse(String fullText, String sourceName) {
        if (fullText == null || fullText.isBlank()) {
            return List.of();
        }
        List<ParsedItem> items = new ArrayList<>();

        // 1) 切条款
        List<Article> articles = splitToArticles(fullText);
        log.info("[SuperiorLawParser] 源={} 切出 {} 个条款", sourceName, articles.size());

        for (Article a : articles) {
            // 2) 授权条款优先
            for (String kw : AUTHORIZATION_KEYWORDS) {
                if (a.text.contains(kw)) {
                    items.add(ParsedItem.builder()
                        .articleNo(a.no)
                        .articleText(a.text)
                        .reason("含授权条款:「" + kw + "」,下位法需细化")
                        .type("AUTHORIZATION")
                        .keyword(kw)
                        .build());
                    break; // 一个条款只入一次
                }
            }
            // 3) 裁量动词(跳过已在 authorization 的)
            boolean isAuth = items.stream().anyMatch(i -> i.articleNo.equals(a.no) && "AUTHORIZATION".equals(i.type));
            if (isAuth) continue;

            String firstDelibVerb = null;
            for (String v : DELIBERATIVE_VERBS) {
                if (a.text.contains(v)) { firstDelibVerb = v; break; }
            }
            if (firstDelibVerb != null && a.text.length() <= 200) {
                items.add(ParsedItem.builder()
                    .articleNo(a.no)
                    .articleText(a.text)
                    .reason("含裁量动词「" + firstDelibVerb + "」,下位法可补充程序 / 标准 / 范围")
                    .type("DELIBERATIVE")
                    .keyword(firstDelibVerb)
                    .build());
            }
        }
        return items;
    }

    private List<Article> splitToArticles(String text) {
        List<Article> articles = new ArrayList<>();
        Matcher m = ARTICLE_PAT.matcher(text);
        while (m.find()) {
            String no = m.group(1);
            String body = m.group(2);
            if (body == null) body = "";
            // 合并多行空白
            body = body.replaceAll("\\s+", " ").trim();
            if (!body.isEmpty()) {
                articles.add(new Article(no, body));
            }
        }
        return articles;
    }

    /** 解析出的"待细化事项"。 */
    public static class ParsedItem {
        public String articleNo;
        public String articleText;
        public String reason;
        public String type;       // AUTHORIZATION / DELIBERATIVE
        public String keyword;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final ParsedItem i = new ParsedItem();
            public Builder articleNo(String v)   { i.articleNo = v;    return this; }
            public Builder articleText(String v) { i.articleText = v;  return this; }
            public Builder reason(String v)      { i.reason = v;       return this; }
            public Builder type(String v)        { i.type = v;         return this; }
            public Builder keyword(String v)     { i.keyword = v;      return this; }
            public ParsedItem build()            { return i; }
        }
    }

    private static class Article {
        final String no;
        final String text;
        Article(String no, String text) { this.no = no; this.text = text; }
    }
}
