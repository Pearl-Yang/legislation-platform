package com.legal.legislation.crawler;

import com.legal.legislation.entity.Regulation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * gov.cn 国务院政策文件库 抓取适配器。
 *
 * 入口: https://www.gov.cn/zhengce/zhengceku/  列表 + 分页
 * 实际页结构:
 *   - 列表页: <a href="/zhengce/content/xxxxx.htm">xxx</a>
 *   - 详情页: <h1 class="article-title"> 标题 </h1>
 *             <td class="pages_content">正文</td>  之类
 *             顶部: <span class="pages_print">发文机关:xxx 发文字号:xxx</span>
 *
 * 本类对真实 DOM 不变式不强依赖:若选择器失效,只返回空列表,不抛异常。
 */
@Component
public class GovCnSource implements RegulationSource {

    private static final Pattern DATE_PAT = Pattern.compile("(\\d{4})[\\-年](\\d{1,2})[\\-月](\\d{1,2})");

    @Override public String name()      { return "gov.cn"; }
    @Override public String entryUrl()  { return "https://www.gov.cn/zhengce/zhengceku/"; }

    @Override
    public List<String> extractDetailUrls(String listHtml, int pageNo) {
        if (listHtml == null || listHtml.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(listHtml, "https://www.gov.cn");
            // 抓所有看起来像 "content/xxxxx.htm" 的链接
            Elements as = doc.select("a[href~=/zhengce/content/\\d+\\.htm]");
            for (Element a : as) {
                String href = a.absUrl("href");
                if (href.isEmpty()) href = a.attr("href");
                if (href.startsWith("/")) href = "https://www.gov.cn" + href;
                if (href.contains("/zhengce/content/") && href.endsWith(".htm")) {
                    if (!out.contains(href)) out.add(href);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    @Override
    public Regulation parseDetail(String detailHtml, String fullUrl) {
        Regulation r = new Regulation();
        r.setSourceUrl(fullUrl);
        r.setRegulationType(Regulation.TYPE_ADMIN_REGULATION);
        r.setStatus(Regulation.STATUS_EFFECTIVE);
        r.setIsFromCrawler(1);
        r.setRegionCode("000000");

        if (detailHtml == null || detailHtml.isEmpty()) {
            r.setRegulationName("(抓取失败:空内容) " + fullUrl);
            return r;
        }
        try {
            Document doc = Jsoup.parse(detailHtml, fullUrl);

            // 标题
            Element title = doc.selectFirst("h1.article-title, h1.title, h1");
            r.setRegulationName(title != null ? title.text().trim() : fullUrl);

            // 发文字号 + 发文机关(粗略)
            Elements spans = doc.select("span");
            StringBuilder meta = new StringBuilder();
            for (Element s : spans) {
                String t = s.text();
                if (t.contains("发文字号") || t.contains("发文机关") || t.contains("发布日期")) {
                    meta.append(t).append("  ");
                }
            }
            String metaText = meta.toString();
            Pattern numPat = Pattern.compile("第[\\s\\S]{0,30}号");
            Matcher m = numPat.matcher(metaText);
            if (m.find()) r.setIssueNumber(m.group().trim());
            Pattern orgPat = Pattern.compile("发文机关[:：]\\s*([^\\s,，。]+)");
            Matcher m2 = orgPat.matcher(metaText);
            if (m2.find()) r.setIssuingAuthority(m2.group(1).trim());

            // 发布日期
            Matcher dm = DATE_PAT.matcher(metaText);
            if (dm.find()) {
                r.setIssueDate(LocalDate.of(
                    Integer.parseInt(dm.group(1)),
                    Integer.parseInt(dm.group(2)),
                    Integer.parseInt(dm.group(3))
                ));
            }

            // 正文
            Element body = doc.selectFirst("td.pages_content, div.trs_editor_view, div.article-content");
            r.setFullText(body != null ? body.text() : doc.body().text());

            // 摘要(取正文前 200 字)
            String full = r.getFullText() == null ? "" : r.getFullText();
            r.setDigest(full.length() > 200 ? full.substring(0, 200) + "..." : full);
        } catch (Exception ex) {
            r.setRegulationName("(解析失败) " + ex.getClass().getSimpleName() + ": " + fullUrl);
        }
        return r;
    }
}
