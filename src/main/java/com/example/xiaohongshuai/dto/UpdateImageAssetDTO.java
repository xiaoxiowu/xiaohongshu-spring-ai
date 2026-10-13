package com.example.xiaohongshuai.dto;

public record UpdateImageAssetDTO(
        String name,
        String tags,
        String usageScene,
        String aspectRatio,
        String status,
        String remark) {
}
