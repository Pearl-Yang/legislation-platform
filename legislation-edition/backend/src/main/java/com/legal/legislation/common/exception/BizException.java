package com.legal.legislation.common.exception;

import lombok.Getter;

/**
 * 业务异常。
 *
 * 用法:
 *   throw new BizException(40001, "项目不存在");
 *
 * code 段位:
 *   4xxxxx  业务错误
 *   5xxxxx  系统错误
 *   401xx   鉴权 / 权限错误
 *   404xx   资源不存在
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(40000, message);
    }

    /** 静态工厂。 */
    public static BizException notFound(String resource) {
        return new BizException(40400, resource + " 不存在");
    }

    public static BizException badRequest(String message) {
        return new BizException(40001, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(40100, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(40300, message);
    }
}
