package com.legal.legislation.config;

import com.legal.legislation.entity.*;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Knife4j 文档元数据配置
 *
 *  文档访问入口：
 *   - Knife4j UI   ：http://localhost:8083/api/doc.html
 *   - OpenAPI JSON ：http://localhost:8083/api/v3/api-docs
 *   - Swagger UI   ：http://localhost:8083/api/swagger-ui/index.html
 *
 *  说明：
 *   - 使用 OpenApiCustomizer 在 springdoc 完成扫描后再注入业务实体，
 *     覆盖 springdoc 自身只扫描被引用 model 的行为，保证 Models 区展示完整。
 *   - 使用单 OpenAPI 文档 + Controller 上的 @Tag 注解完成模块分组，
 *     避免 Knife4j 4.4 在多 Group 模式下与 @Tag 重复注册的 Duplicate key 报错。
 *   - JWT 安全方案在 Components 中声明，可在 Knife4j UI 里 Authorize。
 */
@Slf4j
@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME_NAME = "Authorization";

    /** 需要暴露到 OpenAPI 的业务实体类（24 个） */
    private static final Class<?>[] ENTITY_CLASSES = {
        LegislativeProject.class,
        LegislativeStage.class,
        LegislativeDeadline.class,
        LegislativeStageTemplate.class,
        LegislativeDraft.class,
        DraftVersionHistory.class,
        ReviewRecord.class,
        ReviewIssue.class,
        ReviewRule.class,
        CleanupTask.class,
        CleanupSuggestion.class,
        Regulation.class,
        RegulationRelation.class,
        EvaluationTask.class,
        EvaluationIndicator.class,
        EvaluationResult.class,
        Consultation.class,
        Opinion.class,
        OpinionReply.class,
        OpinionCategory.class,
        LibraryMaterial.class,
        LibraryTag.class,
        MaterialTag.class,
        MaterialNote.class
    };

    @Bean
    public OpenAPI legislationOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("智立法 · 行政立法智能辅助平台 API")
                .description("""
                    本 API 文档服务行政立法智能辅助平台 9 大模块：
                    00 认证 / 01 立法项目 / 02 草案生成 / 03 智慧审查 / 04 法规清理
                    05 实施评估 / 06 意见征集 / 07 立法资料库 / 08 信息门户

                    团队共享指南见 backend/docs/api-docs.md
                    """)
                .version("v0.1.0")
                .contact(new Contact()
                    .name("立法版研发团队")
                    .email("legislation-team@legal.example.com")
                    .url("https://github.com/CaseGuardian/legislation_edition"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server().url("http://localhost:8083/api").description("本机开发环境"),
                new Server().url("http://test.legislation.local:8083/api").description("测试环境（暂未部署）"),
                new Server().url("https://legislation.example.com/api").description("生产环境（暂未部署）")))
            .components(new Components()
                .addSecuritySchemes(JWT_SCHEME_NAME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name(JWT_SCHEME_NAME)
                        .description("使用 /api/auth/login 登录后，把返回的 token 复制到下方 Authorize 对话框（不要带 'Bearer' 前缀）。")))
            .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME_NAME));
    }

    /**
     * springdoc 提供的扩展点：在 springdoc 完成扫描后回调，
     * 我们把全部业务实体 Schema 注入到 components.schemas。
     */
    @Bean
    public OpenApiCustomizer entitySchemasCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            int addedCount = 0;
            for (Class<?> entity : ENTITY_CLASSES) {
                var allModels = ModelConverters.getInstance().readAll(entity);
                for (var entry : allModels.entrySet()) {
                    if (!components.getSchemas().containsKey(entry.getKey())) {
                        components.addSchemas(entry.getKey(), entry.getValue());
                        addedCount++;
                    }
                }
            }
            log.info("[OpenAPI] 通过 OpenApiCustomizer 注册了 {} 个业务实体 Schema", addedCount);
        };
    }
}