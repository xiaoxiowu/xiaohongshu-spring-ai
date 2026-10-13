package com.example.xiaohongshuai.dto;

public record GenerateNoteDTO(
        Long topicId,
        String title,
        String keywords,
        String audience,
        String direction) {
    public GenerateNoteDTO {
        if (topicId == null && (title == null || title.isBlank())) {
            throw new IllegalArgumentException("topicId or title is required");
        }
    }
}
