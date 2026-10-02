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
 * moj.gov.cn 司法部 / 中国法律法规库 适配器。
 *
 * 入口: https://www.moj.gov.cn/policy/  法规库(实际地址可能动态)
 * 抓取策略: 列表 + 详情结构与 GovCn 类似,做更宽松的 URL 匹配。
 */
@Component
public class MojGovCnSource implements RegulationSource {

    private static final Pattern DATE_PAT = Pattern.compile("(\\d{4})[\\-年](\\d{1,2})[\\-月](\\d{1,2})");

    @Override public String name()     { return "moj.gov.cn"; }
    @Override public String entryUrl() { return "https://www.moj.gov.cn/policy/"; }

    @Override
    public List<String> extractDetailUrls(String listHtml, int pageNo) {
        if (listHtml == null || listHtml.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(listHtml, "https://www.moj.gov.cn");
            Elements as = doc.select("a[href]");
            for (Element a : as) {
                String href = a.absUrl("href");
                if (href.isEmpty()) href = a.attr("href");
                if (href.startsWith("/")) href = "https://www.moj.gov.cn" + href;
                if (href.contains("content") && (href.endsWith(".html") || href.endsWith(".htm"))) {
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
        r.setRegulationType(Regulation.TYPE_DEPT_RULE);
        r.setStatus(Regulation.STATUS_EFFECTIVE);
        r.setIsFromCrawler(1);
        r.setRegionCode("000000");

        if (detailHtml == null || detailHtml.isEmpty()) {
            r.setRegulationName("(抓取失败:空内容) " + fullUrl);
            return r;
        }
        try {
            Document doc = Jsoup.parse(detailHtml, fullUrl);
            Element title = doc.selectFirst("h1, .article-title, .title");
            r.setRegulationName(title != null ? title.text().trim() : fullUrl);

            String all = doc.body().text();
            Matcher dm = DATE_PAT.matcher(all);
            if (dm.find()) {
                r.setIssueDate(LocalDate.of(
                    Integer.parseInt(dm.group(1)),
                    Integer.parseInt(dm.group(2)),
                    Integer.parseInt(dm.group(3))
                ));
            }
            Element body = doc.selectFirst("div.article-content, div.content, td.content");
            r.setFullText(body != null ? body.text() : all);
            String full = r.getFullText() == null ? "" : r.getFullText();
            r.setDigest(full.length() > 200 ? full.substring(0, 200) + "..." : full);
        } catch (Exception ex) {
            r.setRegulationName("(解析失败) " + ex.getClass().getSimpleName());
        }
        return r;
    }
}
