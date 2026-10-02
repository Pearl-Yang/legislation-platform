package com.legal.legislation.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 爬取状态存储(JVM 内存版,生产可换 Redis)。
 *
 *  字段:
 *   - checkpoints : source → 最新 CrawlerCheckpoint
 *   - doneUrls    : source → 已成功抓取的 URL 集合(去重用)
 *
 *  注意:doneUrls 在 JVM 重启后会清空,设计上有意:从 checkpoint.lastPageNo 之后
 *        所有 URL 都重新尝试一遍,数据库层(source_url 唯一)保证不会重复入库。
 */
@Slf4j
@Component
public class CrawlerCheckpointStore {

    private final Map<String, CrawlerCheckpoint> checkpoints = new ConcurrentHashMap<>();
    private final Map<String, Set<String>>       doneUrls    = new ConcurrentHashMap<>();

    public CrawlerCheckpoint loadOrInit(String source) {
        return checkpoints.computeIfAbsent(source, k ->
            CrawlerCheckpoint.builder()
                .source(k)
                .lastPageNo(1)
                .totalCount(0)
                .status("PENDING")
                .updatedAt(LocalDateTime.now())
                .build()
        );
    }

    public void save(CrawlerCheckpoint cp) {
        checkpoints.put(cp.getSource(), cp);
        log.info("[Crawler] checkpoint saved source={} status={} page={} total={}",
            cp.getSource(), cp.getStatus(), cp.getLastPageNo(), cp.getTotalCount());
    }

    public boolean isAlreadyCrawled(String source, String url) {
        return doneUrls.computeIfAbsent(source, k -> ConcurrentHashMap.newKeySet()).contains(url);
    }

    public void markCrawled(String source, String url) {
        doneUrls.computeIfAbsent(source, k -> ConcurrentHashMap.newKeySet()).add(url);
    }

    public CrawlerCheckpoint get(String source) { return checkpoints.get(source); }
    public Map<String, CrawlerCheckpoint> all() { return Map.copyOf(checkpoints); }
}
