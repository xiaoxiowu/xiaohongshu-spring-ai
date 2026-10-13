package com.example.xiaohongshuai.vo;

import java.util.List;

public record SystemConfigVO(
        ModelConfig model,
        McpConfig mcp,
        CrawlerConfig crawler) {

    public record ModelConfig(
            String provider,
            String baseUrl,
            String model,
            Double temperature,
            boolean apiKeyConfigured) {
    }

    public record McpConfig(
            boolean enabled,
            String clientName,
            String clientType,
            String requestTimeout,
            List<McpConnection> connections,
            Integer toolCount,
            String errorMessage) {
    }

    public record McpConnection(
            String name,
            String type,
            String url,
            String endpoint,
            String configuration) {
    }

    public record CrawlerConfig(
            String topicAnalysisProvider,
            String mediaCrawlerWorkDir,
            String mediaCrawlerCommand,
            boolean mediaCrawlerGetComments,
            Integer mediaCrawlerMaxCommentsPerNote) {
    }
}
