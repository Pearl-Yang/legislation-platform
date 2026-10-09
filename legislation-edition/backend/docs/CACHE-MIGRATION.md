# CacheConfig · 内存 ↔ Redis 切换手册

> 当前实现: `ConcurrentMapCacheManager`(JVM 内存,重启即失效)
> 切换目标: `RedisCacheManager`(多实例共享)

---

## 1. 何时需要切

- 多 backend 实例部署 (横向扩展) 时,内存缓存各实例独立,数据不一致
- 需要跨重启保留缓存
- 需要命中率监控 / 失效策略更精细

不切也能用：
- 单实例 (答辩 / 演示)
- 数据可重建 (dashboard 聚合几秒就出)

---

## 2. 切换步骤

### 2.1 加依赖

`legislation-edition/backend/pom.xml` 取消注释 (已注释)：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2.2 加配置

`application.yml`：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 2s

legislation:
  cache:
    type: redis     # 切到 redis

management:
  redis:
    metrics:
      enabled: true
```

### 2.3 改 CacheConfig

把当前 `ConcurrentMapCacheManager` 替换为 `RedisCacheManager`：

```java
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "legislation.cache", name = "type", havingValue = "redis")
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))         // 默认 5 分钟过期
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues()
            .prefixCacheNameWith("legislation:");

        // 不同 cacheNames 用不同 TTL
        Map<String, RedisCacheConfiguration> perCache = Map.of(
            "project:dashboard", cfg.entryTtl(Duration.ofMinutes(1)),  // 仪表盘 1 分钟
            "regulation:index",  cfg.entryTtl(Duration.ofHours(1)),    // 法规索引 1 小时
            "info:dashboard",    cfg.entryTtl(Duration.ofMinutes(5)),
            "evaluation:chart",  cfg.entryTtl(Duration.ofMinutes(10))
        );
        return RedisCacheManager.builder(factory)
            .cacheDefaults(cfg)
            .withInitialCacheConfigurations(perCache)
            .transactionAware()
            .build();
    }
}
```

同时在原 `CacheConfig` 上保留 `@ConditionalOnProperty(matchIfMissing = true)`(默认 memory)，加 `RedisCacheConfig` 用 `havingValue = "redis"`。两个配置按 `legislation.cache.type` 二选一。

### 2.4 docker-compose 加 redis 服务

`docker/docker-compose.yml`：

```yaml
  redis:
    image: redis:7-alpine
    container_name: legislation-redis
    ports: ['6379:6379']
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

volumes:
  redis-data:
```

backend 容器 `depends_on` 加 `condition: service_healthy redis`。

### 2.5 验证

```bash
docker compose up -d redis
docker compose up -d backend --force-recreate

# 触发一次 dashboard 写入缓存
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/info/dashboard

# 查 redis 中有没有 key
docker exec legislation-redis redis-cli KEYS 'legislation:*'
# 应该看到 legislation:project:dashboard::global
```

---

## 3. 当前缓存键

| Cache Name | 写入位置 | 默认 TTL | 失效时机 |
|------------|----------|----------|----------|
| `project:dashboard` | `LegislativeProjectServiceImpl.dashboard` `@Cacheable` | 内存无限 | 无显式 evict,改项目时需注意 |
| `regulation:index` | `InfoServiceImpl.regulationIndex` `@Cacheable` | 内存无限 | 同上 |
| `info:dashboard` | `InfoServiceImpl.dashboard` `@Cacheable` | 内存无限 | 同上 |
| `evaluation:chart` | 预留(暂未使用) | 内存无限 | — |

> **生产注意**: 内存模式下如果 Service 实现里没有 `@CacheEvict`,改数据后缓存不会失效,会展示旧数据。Redis 模式下可用 TTL 自动兜底。

---

## 4. 监控

`/actuator/metrics/cache.gets` 暴露缓存命中/未命中次数：

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8083/api/actuator/metrics/cache.gets
```

Grafana 看板建议加：
- `cache_gets_total` 命中数
- `cache_puts_total` 写入数
- `cache_evictions_total` 驱逐数(Redis 模式才有意义)
