package com.legal.legislation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动入口。
 * <p>
 * 继承 {@link SpringBootServletInitializer} 使本程序既能：
 * <ul>
 *   <li>本地通过 {@code java -jar} 直接运行（内嵌 Tomcat，pom 切换 packaging=jar 即可）</li>
 *   <li>打成 war 部署到外置 Tomcat 9.x / 10.x（推荐团队成员使用的方式）</li>
 * </ul>
 * <p>
 * 注意：当部署到外置 Tomcat 时，{@code server.port} 和 {@code server.servlet.context-path}
 * 由 Tomcat 的 server.xml 和 war 文件名决定，application.yml 里的这两个配置会被忽略。
 */
@SpringBootApplication(exclude = {
    Neo4jDataAutoConfiguration.class,
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class,
    QuartzAutoConfiguration.class
})
@MapperScan("com.legal.legislation.mapper")
@EnableScheduling
@EnableAsync
public class LegislationApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(LegislationApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(LegislationApplication.class);
    }
}