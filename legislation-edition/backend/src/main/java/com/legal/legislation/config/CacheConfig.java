package com.legal.legislation.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 缓存配置 - 当前使用 JVM 内存缓存(ConcurrentMapCacheManager)。
 *
 * 切到 Redis 只需:
 *   1. 加 spring-boot-starter-data-redis 依赖
 *   2. 在 application.yml 配 spring.data.redis.*
 *   3. 把本类换成 RedisCacheManager 创建方式
 *   4. 删除 @ConditionalOnProperty
 *
 * 缓存名:
 *   - project:dashboard : 项目仪表盘聚合
 *   - regulation:index : 法规索引
 *   - info:dashboard   : 信息门户 dashboard
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "legislation.cache", name = "type", havingValue = "memory", matchIfMissing = true)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager mgr = new ConcurrentMapCacheManager();
        mgr.setCacheNames(List.of(
            "project:dashboard",
            "regulation:index",
            "info:dashboard",
            "evaluation:chart"
        ));
        return mgr;
    }
}
