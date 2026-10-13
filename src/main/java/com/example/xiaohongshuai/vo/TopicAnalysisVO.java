package com.example.xiaohongshuai.vo;

import java.util.List;

public record TopicAnalysisVO(
        String keyword,
        List<FeedSample> samples,
        String summary,
        List<TopicRecommendation> recommendations,
        String rawSearchResponse,
        String summaryError) {

    public record FeedSample(
            String feedId,
            String xsecToken,
            String title,
            String content,
            String author,
            String url,
            Integer likes,
            Integer collects,
            Integer comments,
            Integer shares,
            List<String> commentSamples,
            String rawDetailResponse) {
    }

    public record TopicRecommendation(
            String title,
            String keywords,
            String audience,
            String direction) {
    }
}
