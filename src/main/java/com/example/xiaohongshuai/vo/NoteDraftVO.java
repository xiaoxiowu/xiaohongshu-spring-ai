package com.example.xiaohongshuai.vo;

import java.time.LocalDateTime;
import java.util.List;

import com.example.xiaohongshuai.common.StatusText;
import com.example.xiaohongshuai.entity.NoteDraft;

public record NoteDraftVO(
        Long id,
        Long topicId,
        String title,
        String content,
        String tags,
        String imagePrompt,
        List<Long> imageAssetIds,
        String status,
        String statusText,
        String publishedNoteId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static NoteDraftVO from(NoteDraft draft) {
        return new NoteDraftVO(draft.getId(), draft.getTopicId(), draft.getTitle(), draft.getContent(),
                draft.getTags(), draft.getImagePrompt(), parseImageAssetIds(draft.getImageAssetIds()),
                draft.getStatus(), StatusText.status(draft.getStatus()),
                draft.getPublishedNoteId(), draft.getCreatedAt(), draft.getUpdatedAt());
    }

    private static List<Long> parseImageAssetIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
