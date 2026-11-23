package com.syh.imhub.common.result;

public record R<T>(int code, String msg, T data, String traceId) {

    // 成功
    public static <T> R<T> ok(T data) {
        return new R<>(0, "ok", data, null);
    }
    // 无数据成功
    public static <T> R<T> ok() {
        return new R<>(0, "ok", null, null);
    }
    //失败
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null, null);
    }
}
