package com.demo.common.api;

import lombok.Getter;

/**
 * 统一返回信封：code = 0 表示成功，非 0 表示错误。
 * 方案 A：HTTP 永远 200，业务结果靠 code 区分。
 */
@Getter
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    private Result() {}

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ErrorCode ec) {
        Result<T> r = new Result<>();
        r.code = ec.getCode();
        r.msg = ec.getMsg();
        return r;
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
