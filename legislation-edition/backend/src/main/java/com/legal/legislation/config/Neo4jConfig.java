package com.legal.legislation.config;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Neo4j Driver 单例 Bean。
 *
 * 通过 legislation.neo4j.enabled=true 启用。
 * 默认 false (开发环境不强制要求 Neo4j)。
 */
@Slf4j
@Configuration
public class Neo4jConfig {

    @Value("${spring.neo4j.uri:bolt://localhost:7687}")
    private String uri;

    @Value("${spring.neo4j.authentication.username:neo4j}")
    private String username;

    @Value("${spring.neo4j.authentication.password:neo4j}")
    private String password;

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "legislation.neo4j.enabled", havingValue = "true")
    public Driver neo4jDriver() {
        log.info("[Neo4jConfig] creating Driver to {}", uri);
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
}