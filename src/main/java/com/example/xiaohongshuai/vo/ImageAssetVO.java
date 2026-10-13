package com.example.xiaohongshuai.vo;

import java.time.LocalDateTime;

import com.example.xiaohongshuai.common.StatusText;
import com.example.xiaohongshuai.entity.ImageAsset;

public record ImageAssetVO(
        Long id,
        String name,
        String prompt,
        String tags,
        String usageScene,
        String aspectRatio,
        String source,
        String sourceText,
        String status,
        String statusText,
        String localPath,
        String originalUrl,
        String mcpResponse,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ImageAssetVO from(ImageAsset asset) {
        return new ImageAssetVO(asset.getId(), asset.getName(), asset.getPrompt(), asset.getTags(),
                asset.getUsageScene(), asset.getAspectRatio(), asset.getSource(), StatusText.source(asset.getSource()),
                asset.getStatus(), StatusText.status(asset.getStatus()), asset.getLocalPath(), asset.getOriginalUrl(),
                asset.getMcpResponse(), asset.getRemark(), asset.getCreatedAt(), asset.getUpdatedAt());
    }
}
