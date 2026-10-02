package com.legal.legislation.crawler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单令牌桶限流器(每数据源一个)。
 *
 * 默认 1 token / 1000ms,即每秒 1 个请求。
 * 真实情况下读取 application.yml 配 legislation.crawler.qpsPerSource
 */
public class RateLimiter {

    private final long intervalMs;
    private final AtomicLong nextAllowedAt = new AtomicLong(0);

    private static final ConcurrentHashMap<String, RateLimiter> CACHE = new ConcurrentHashMap<>();

    private RateLimiter(long intervalMs) { this.intervalMs = intervalMs; }

    public static RateLimiter of(String source) {
        return CACHE.computeIfAbsent(source, k -> {
            long qps = 1; // 后续可从 @Value 注入
            return new RateLimiter(Math.max(1, 1000L / Math.max(1, qps)));
        });
    }

    public void acquire() {
        long now = System.currentTimeMillis();
        long allowed;
        while (true) {
            allowed = nextAllowedAt.get();
            if (now >= allowed) {
                if (nextAllowedAt.compareAndSet(allowed, now + intervalMs)) return;
            } else {
                try { Thread.sleep(allowed - now); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }
}
