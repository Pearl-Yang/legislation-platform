package com.legal.legislation.common;

import com.legal.legislation.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通用响应 / 业务异常 单元测试。
 *
 * 覆盖:
 *  - Result.success/error 工厂方法
 *  - BizException 工厂方法和 code 段位约定
 *  - 异常 message 透传
 */
class ResultAndBizExceptionTest {

    // ---------- Result ----------

    @Test
    @DisplayName("Result.success() 默认 code=200, message=操作成功, data=null")
    void result_successDefault() {
        Result<String> r = Result.success();
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMessage()).isEqualTo("操作成功");
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("Result.success(data) 透传 data")
    void result_successWithData() {
        Result<Integer> r = Result.success(42);
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getData()).isEqualTo(42);
    }

    @Test
    @DisplayName("Result.error(code, message) 设置 code/message, data=null")
    void result_errorWithCode() {
        Result<String> r = Result.error(40400, "资源不存在");
        assertThat(r.getCode()).isEqualTo(40400);
        assertThat(r.getMessage()).isEqualTo("资源不存在");
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("Result.error(String) 默认 50000 业务码")
    void result_errorDefaultCode() {
        Result<String> r = Result.error("boom");
        assertThat(r.getCode()).isEqualTo(50000);
        assertThat(r.getMessage()).isEqualTo("boom");
    }

    // ---------- BizException ----------

    @Test
    @DisplayName("BizException 工厂方法 notFound -> 40400")
    void bizException_notFound() {
        BizException ex = BizException.notFound("项目");
        assertThat(ex.getCode()).isEqualTo(40400);
        assertThat(ex.getMessage()).isEqualTo("项目 不存在");
    }

    @Test
    @DisplayName("BizException.badRequest -> 40001")
    void bizException_badRequest() {
        BizException ex = BizException.badRequest("参数错");
        assertThat(ex.getCode()).isEqualTo(40001);
    }

    @Test
    @DisplayName("BizException.unauthorized -> 40100, forbidden -> 40300")
    void bizException_auth() {
        assertThat(BizException.unauthorized("未登录").getCode()).isEqualTo(40100);
        assertThat(BizException.forbidden("无权限").getCode()).isEqualTo(40300);
    }

    @Test
    @DisplayName("BizException 是 RuntimeException, 抛出可被 Spring @RestControllerAdvice 捕获")
    void bizException_isRuntime() {
        assertThatThrownBy(() -> {
            throw BizException.badRequest("x");
        }).isInstanceOf(RuntimeException.class);
    }
}
