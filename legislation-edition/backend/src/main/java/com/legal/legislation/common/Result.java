package com.legal.legislation.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构。
 *
 *   {code: 200, message: "操作成功", data: ...}
 *
 * code 约定:
 *   200         成功
 *   4xxxxx      业务失败
 *   5xxxxx      系统错误
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一响应")
public class Result<T> implements Serializable {

    @Schema(description = "业务码:200 成功;4xxxxx 业务失败;5xxxxx 系统错误")
    private Integer code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("操作成功");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(50000, message);
    }
}
