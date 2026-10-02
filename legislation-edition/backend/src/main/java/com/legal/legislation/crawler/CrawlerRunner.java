package com.legal.legislation.crawler;

import com.legal.legislation.entity.Regulation;
import com.legal.legislation.mapper.RegulationMapper;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 通用法规爬取 Runner。
 *
 * 关键设计:
 *   1. 限流:  每个数据源一个 RateLimiter,默认 1 request/秒,可在 application.yml 调
 *   2. 失败重试: HTTP 请求最多重试 3 次,指数退避
 *   3. 断点续抓: 通过 CrawlerCheckpointStore 持久化状态
 *      - 同一 source 重启时,从 lastPageNo 续抓
 *      - 同一 source 同一 url 已成功过则跳过(去重)
 *   4. 超时:  默认 15s,可调
 *
 * 入口:
 *   - run(source)            : 跑完整个 source
 *   - runOnePage(source, n)  : 只跑第 N 页(用于手动/分批)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerRunner {

    private final RegulationMapper         regulationMapper;
    private final CrawlerCheckpointStore   checkpointStore;
    private final Map<String, Object>      sourceLocks = new ConcurrentHashMap<>();

    /** 暴露给 controller 读状态 */
    public CrawlerCheckpointStore checkpointStore() { return checkpointStore; }

    /** 抓取并入库。返回报告。 */
    public CrawlerReport run(RegulationSource source) {
        return run(source, 0, Integer.MAX_VALUE);
    }

    public CrawlerReport run(RegulationSource source, int fromPage, int toPage) {
        long t0 = System.currentTimeMillis();
        int pages = 0, detailUrls = 0, saved = 0, failed = 0;
        String err = null;
        try {
            CrawlerCheckpoint cp = checkpointStore.loadOrInit(source.name());
            int start = Math.max(fromPage, cp.getLastPageNo());
            int end   = Math.min(toPage, source.totalPages());
            log.info("[Crawler] {} start page={}→{}", source.name(), start, end);

            for (int p = start; p <= end; p++) {
                pages++;
                String listUrl = buildListUrl(source, p);
                String listHtml = fetchWithRetry(listUrl, 3);
                if (listHtml == null) {
                    failed++;
                    continue;
                }
                List<String> urls = source.extractDetailUrls(listHtml, p);
                detailUrls += urls.size();
                for (String url : urls) {
                    if (checkpointStore.isAlreadyCrawled(source.name(), url)) {
                        continue;
                    }
                    RateLimiter.of(source.name()).acquire();
                    String detailHtml = fetchWithRetry(url, 3);
                    if (detailHtml == null) {
                        failed++;
                        continue;
                    }
                    Regulation r = source.parseDetail(detailHtml, url);
                    try {
                        saveRegulation(r);
                        checkpointStore.markCrawled(source.name(), url);
                        saved++;
                    } catch (Exception ex) {
                        log.warn("[Crawler] save failed url={} msg={}", url, ex.getMessage());
                        failed++;
                    }
                }
                // 成功翻一页
                checkpointStore.save(CrawlerCheckpoint.builder()
                    .source(source.name())
                    .lastUrl(listUrl)
                    .lastPageNo(p)
                    .totalCount(cp.getTotalCount() + saved)
                    .status("RUNNING")
                    .updatedAt(LocalDateTime.now())
                    .build());
            }
            // 全部完成
            checkpointStore.save(CrawlerCheckpoint.builder()
                .source(source.name())
                .lastPageNo(end)
                .totalCount(cp.getTotalCount() + saved)
                .status("SUCCESS")
                .updatedAt(LocalDateTime.now())
                .build());
        } catch (Exception ex) {
            err = ex.getMessage();
            log.error("[Crawler] {} failed", source.name(), ex);
            checkpointStore.save(CrawlerCheckpoint.builder()
                .source(source.name())
                .status("FAILED")
                .errorMessage(err)
                .updatedAt(LocalDateTime.now())
                .build());
        }
        return new CrawlerReport(source.name(), pages, detailUrls, saved, failed, System.currentTimeMillis() - t0, err);
    }

    private String buildListUrl(RegulationSource src, int pageNo) {
        // 各 source 自带 entryUrl,简单实现:entryUrl?page=N
        // 真实情况下每个 source 会重写此方法
        if (pageNo <= 1) return src.entryUrl();
        return src.entryUrl() + (src.entryUrl().contains("?") ? "&" : "?") + "page=" + pageNo;
    }

    /** 失败重试,带指数退避 */
    private String fetchWithRetry(String url, int maxRetries) {
        int attempt = 0;
        long backoff = 800;
        while (attempt < maxRetries) {
            try {
                Connection conn = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .referrer("https://www.gov.cn/")
                    .timeout((int) Duration.ofSeconds(15).toMillis())
                    .ignoreHttpErrors(true)
                    .followRedirects(true);
                Connection.Response resp = conn.execute();
                if (resp.statusCode() == 200) {
                    return resp.body();
                }
                log.warn("[Crawler] {} status={} attempt={}", url, resp.statusCode(), attempt + 1);
            } catch (IOException ex) {
                log.warn("[Crawler] {} attempt={} failed: {}", url, attempt + 1, ex.getMessage());
            }
            attempt++;
            try { TimeUnit.MILLISECONDS.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            backoff *= 2;
        }
        return null;
    }

    /** 去重:已存在同 sourceUrl 则更新,否则插入 */
    private void saveRegulation(Regulation r) {
        // 极简去重:用 source_url 作唯一键
        if (r.getSourceUrl() == null) {
            regulationMapper.insert(r);
            return;
        }
        Regulation exist = regulationMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Regulation>()
                .eq("source_url", r.getSourceUrl())
                .last("LIMIT 1")
        );
        if (exist == null) {
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            regulationMapper.insert(r);
        } else {
            r.setId(exist.getId());
            r.setUpdatedAt(LocalDateTime.now());
            regulationMapper.updateById(r);
        }
    }
}
