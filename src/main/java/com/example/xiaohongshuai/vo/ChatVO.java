package com.example.xiaohongshuai.vo;

public record ChatVO(
        String content,
        String toolName,
        Object toolResult) {

    public ChatVO(String content) {
        this(content, null, null);
    }
}
