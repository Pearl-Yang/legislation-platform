package com.legal.legislation.crawler;

import com.legal.legislation.entity.Regulation;

import java.util.List;

/**
 * 法规数据源抓取适配器。
 *
 * 每个数据源(国务院 / 司法部 / 某省政府)各实现一个,关注:
 *  - 列表页 URL 模板
 *  - 列表页 → 详情页 URL 抽取规则
 *  - 详情页 → Regulation 字段映射
 *
 * 框架 CrawlerRunner 负责限流 / 失败重试 / 断点续抓。
 */
public interface RegulationSource {

    /** 唯一标识,例如 "gov.cn/zhengce/2024" */
    String name();

    /** 列表页入口 URL(框架从这一页开始抓) */
    String entryUrl();

    /**
     * 从列表页 HTML 抽出本批次要进入详情的链接。
     * @param pageNo 当前是第几页
     * @return 详情页 URL 列表(可以是相对路径)
     */
    List<String> extractDetailUrls(String listHtml, int pageNo);

    /** 详情页 HTML → Regulation 对象 */
    Regulation parseDetail(String detailHtml, String fullUrl);

    /** 总列表页数(用于分页;若不可知,返回 Integer.MAX_VALUE) */
    default int totalPages() { return Integer.MAX_VALUE; }
}
