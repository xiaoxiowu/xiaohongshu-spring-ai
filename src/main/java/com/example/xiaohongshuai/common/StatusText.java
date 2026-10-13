package com.example.xiaohongshuai.common;

import java.util.Map;

public final class StatusText {

    public static final Map<String, String> STATUS = Map.ofEntries(
            Map.entry("TODO", "待生成"),
            Map.entry("GENERATED", "已生成"),
            Map.entry("ARCHIVED", "已归档"),
            Map.entry("DRAFT", "草稿"),
            Map.entry("READY", "待发布"),
            Map.entry("PUBLISHING", "发布中"),
            Map.entry("PUBLISHED", "已发布"),
            Map.entry("PENDING", "待处理"),
            Map.entry("RUNNING", "执行中"),
            Map.entry("SUCCESS", "成功"),
            Map.entry("FAILED", "失败"),
            Map.entry("ACTIVE", "可用"),
            Map.entry("LOGGED_IN", "已登录"),
            Map.entry("NOT_LOGGED_IN", "未登录"),
            Map.entry("MCP_UNAVAILABLE", "MCP不可用"),
            Map.entry("ERROR", "异常"),
            Map.entry("UNKNOWN", "未知")
    );

    public static final Map<String, String> SOURCE = Map.of(
            "JIMENG", "即梦生成",
            "UPLOAD", "本地上传",
            "URL", "外部链接"
    );

    private StatusText() {
    }

    public static String status(String value) {
        return STATUS.getOrDefault(value, value);
    }

    public static String source(String value) {
        return SOURCE.getOrDefault(value, value);
    }
}
