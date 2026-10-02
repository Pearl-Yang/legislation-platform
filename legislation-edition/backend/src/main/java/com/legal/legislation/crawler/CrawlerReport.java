package com.legal.legislation.crawler;

/**
 * 爬取结果汇总。
 */
public class CrawlerReport {
    public final String source;
    public final int    pages;
    public final int    detailUrls;
    public final int    saved;
    public final int    failed;
    public final long   elapsedMs;
    public final String error;

    public CrawlerReport(String source, int pages, int detailUrls, int saved, int failed, long elapsedMs, String error) {
        this.source     = source;
        this.pages      = pages;
        this.detailUrls = detailUrls;
        this.saved      = saved;
        this.failed     = failed;
        this.elapsedMs  = elapsedMs;
        this.error      = error;
    }
}
