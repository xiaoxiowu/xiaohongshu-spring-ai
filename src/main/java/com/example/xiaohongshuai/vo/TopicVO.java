package com.example.xiaohongshuai.vo;

import java.time.LocalDateTime;

import com.example.xiaohongshuai.common.StatusText;
import com.example.xiaohongshuai.entity.Topic;

public record TopicVO(
        Long id,
        String title,
        String keywords,
        String audience,
        String direction,
        String status,
        String statusText,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TopicVO from(Topic topic) {
        return new TopicVO(topic.getId(), topic.getTitle(), topic.getKeywords(), topic.getAudience(),
                topic.getDirection(), topic.getStatus(), StatusText.status(topic.getStatus()),
                topic.getCreatedAt(), topic.getUpdatedAt());
    }
}
