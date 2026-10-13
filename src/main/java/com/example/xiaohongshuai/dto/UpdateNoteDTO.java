package com.example.xiaohongshuai.dto;

import java.util.List;

public record UpdateNoteDTO(
        String title,
        String content,
        String tags,
        String imagePrompt,
        List<Long> imageAssetIds,
        String status) {
}
