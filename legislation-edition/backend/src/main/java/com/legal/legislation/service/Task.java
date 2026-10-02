package com.legal.legislation.service;

import lombok.Data;

/**
 * Service 层统一返回包装
 * 这里使用与 controller/Result 等价的轻量级类型，避免与 common.Result 重复。
 */
@Data
public class Task<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Task<T> ok(T data) {
        Task<T> t = new Task<>();
        t.code = 200;
        t.message = "ok";
        t.data = data;
        return t;
    }

    public static <T> Task<T> error(String message) {
        Task<T> t = new Task<>();
        t.code = 500;
        t.message = message;
        return t;
    }

    public boolean isSuccess() {
        return code != null && code == 200;
    }
}