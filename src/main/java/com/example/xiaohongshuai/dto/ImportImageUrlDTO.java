package com.example.xiaohongshuai.dto;

public record ImportImageUrlDTO(
        String name,
        String url,
        String prompt,
        String tags,
        String usageScene,
        String aspectRatio,
        String remark) {
}
