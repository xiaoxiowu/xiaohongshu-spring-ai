package com.example.xiaohongshuai.vo;

import java.time.LocalDateTime;
import java.util.List;

public record TopicAnalysisJobVO(
        String id,
        String status,
        String keyword,
        List<String> logs,
        List<TopicAnalysisVO.FeedSample> samples,
        TopicAnalysisVO result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime finishedAt) {
}
