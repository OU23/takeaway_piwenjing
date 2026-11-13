package com.xingguang.delivery.common;

import java.util.UUID;

/**
 * 链路追踪 ID（写入响应体 traceId 字段，便于前后端联查日志）。
 */
public final class TraceId {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceId() {
    }

    public static String current() {
        String id = HOLDER.get();
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            HOLDER.set(id);
        }
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
