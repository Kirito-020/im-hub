package com.syh.imhub.common.util;

public class TraceId {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<String>();

    public static String get() {
        return HOLDER.get();
    }
}
