package com.example.xiaohongshuai.dto;

public record GenerateImageDTO(
        String name,
        String prompt,
        String tags,
        String usageScene,
        String aspectRatio,
        String remark) {
}
