package com.legal.legislation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 端到端集成测试 - H2 + openapi profile,不依赖 MySQL/Neo4j。
 *
 * 覆盖:
 *  - /actuator/health       公开
 *  - /auth/health           公开
 *  - /auth/login 失败路径   公开,验证 BizException 序列化
 *  - /legislative-project/list 无 token 时 401/403
 *  - /admin/crawler/status  无 token 时 401/403
 *  - /v3/api-docs(OpenAPI 文档可生成)
 *  - /doc.html (Knife4j UI)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("openapi")
class IntegrationTest {

    @Autowired private MockMvc mvc;

    @Test
    void actuator_health_isUp() throws Exception {
        mvc.perform(get("/actuator/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void auth_health_returnsOk() throws Exception {
        mvc.perform(get("/auth/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void auth_login_wrongPassword_returnsUnauthorized() throws Exception {
        // BizException.unauthorized 使用默认业务错误码 40100
        mvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
           .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void auth_login_adminCorrectPassword_returnsJwtToken() throws Exception {
        // V1__init_schema.sql 已注入 admin / INIT 等价 123456,DataInitializer 自动回填 BCrypt
        // 第一次正确登录会让 AuthService 的"密码为空/INIT 占位则按密码回填"分支被触发,这是预期行为
        mvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
           .andExpect(jsonPath("$.code").value(200))
           .andExpect(jsonPath("$.data.token").exists())
           .andExpect(jsonPath("$.data.token").isString())
           .andExpect(jsonPath("$.data.username").value("admin"))
           .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void auth_login_allSeedUsers_pass() throws Exception {
        // V1__init_schema.sql 注入 admin / user1 (id=1, 2)
        // V3__seed_five_role_users.sql 注入 leader / drafter / reviewer / evaluator / admin(role 不冲突)
        // DataInitializer 会把所有 password_hash = 'INIT' 的用户回填为 BCrypt(123456)
        String[][] users = {
            {"admin",     "ROLE_ADMIN"},
            {"user1",     "USER"},
            {"leader",    "ROLE_LEADER"},
            {"drafter",   "ROLE_USER"},
            {"reviewer",  "ROLE_REVIEWER"},
            {"evaluator", "ROLE_EVALUATOR"}
        };
        for (String[] u : users) {
            mvc.perform(post("/auth/login")
                    .contentType("application/json")
                    .content("{\"username\":\"" + u[0] + "\",\"password\":\"123456\"}"))
               .andExpect(jsonPath("$.code").value(200))
               .andExpect(jsonPath("$.data.token").isNotEmpty())
               .andExpect(jsonPath("$.data.username").value(u[0]));
        }
    }

    @Test
    void auth_login_missingUsername_returnsBadRequest() throws Exception {
        // BizException.badRequest 使用默认业务错误码 40001
        mvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"password\":\"x\"}"))
           .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void businessEndpoint_withoutToken_returns4xx() throws Exception {
        mvc.perform(get("/legislative-project/list"))
           .andExpect(status().is4xxClientError());
    }

    @Test
    void adminEndpoint_withoutToken_returns4xx() throws Exception {
        mvc.perform(get("/admin/crawler/status"))
           .andExpect(status().is4xxClientError());
    }

    @Test
    void openapiDocs_areGenerated() throws Exception {
        // openapi profile 下 springdoc-openapi 应能生成完整 OpenAPI 3 文档
        mvc.perform(get("/v3/api-docs"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.openapi").exists())
           .andExpect(jsonPath("$.paths").exists());
    }

    @Test
    void knife4jDocPage_loads() throws Exception {
        mvc.perform(get("/doc.html"))
           .andExpect(status().isOk());
    }

    @Test
    void prometheusEndpoint_exposesMetrics() throws Exception {
        // 触发 actuator/prometheus,确保指标可被 scrape
        mvc.perform(get("/actuator/prometheus"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith("text/plain"))
           // Prometheus 默认会暴露 jvm_memory_used_bytes 等基础指标
           .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")));
    }
}