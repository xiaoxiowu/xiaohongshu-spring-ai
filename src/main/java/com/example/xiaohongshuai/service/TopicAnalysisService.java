package com.example.xiaohongshuai.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.example.xiaohongshuai.config.McpRegistry;
import com.example.xiaohongshuai.vo.TopicAnalysisVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TopicAnalysisService {

    private final McpClientService mcpClientService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final MediaCrawlerTopicService mediaCrawlerTopicService;
    private final String provider;

    @Autowired
    public TopicAnalysisService(McpClientService mcpClientService, ChatService chatService,
            ObjectMapper objectMapper, MediaCrawlerTopicService mediaCrawlerTopicService,
            @Value("${xhs.topic-analysis.provider:mediacrawler}") String provider) {
        this.mcpClientService = mcpClientService;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
        this.mediaCrawlerTopicService = mediaCrawlerTopicService;
        this.provider = provider;
    }

    TopicAnalysisService(McpClientService mcpClientService, ChatService chatService, ObjectMapper objectMapper) {
        this(mcpClientService, chatService, objectMapper, null, "mcp");
    }

    public TopicAnalysisVO search(String keyword, Integer limit, String sortBy, String noteType, String publishTime) {
        return search(keyword, limit, sortBy, noteType, publishTime, ignored -> {
        });
    }

    public TopicAnalysisVO search(String keyword, Integer limit, String sortBy, String noteType, String publishTime,
            Consumer<String> progress) {
        return search(keyword, limit, sortBy, noteType, publishTime, progress, ignored -> {
        });
    }

    public TopicAnalysisVO search(String keyword, Integer limit, String sortBy, String noteType, String publishTime,
            Consumer<String> progress, Consumer<List<TopicAnalysisVO.FeedSample>> sampleProgress) {
        int actualLimit = limit == null ? 10 : Math.max(1, Math.min(limit, 20));
        if ("mediacrawler".equalsIgnoreCase(provider)) {
            if (mediaCrawlerTopicService == null) {
                throw new IllegalStateException("MediaCrawler service is not available");
            }
            MediaCrawlerTopicService.MediaCrawlerResult result = mediaCrawlerTopicService.search(keyword, actualLimit,
                    sortBy, noteType, publishTime, progress, sampleProgress);
            progress.accept("采集到 " + result.samples().size() + " 条样本，开始 AI 总结");
            SummaryResult summary = summarize(keyword, result.samples());
            return new TopicAnalysisVO(keyword, result.samples(), summary.content(), summary.recommendations(),
                    result.rawSearchResponse(), summary.errorMessage());
        }
        progress.accept("当前配置为 MCP 数据源，开始调用 MCP 搜索");
        return searchByMcp(keyword, actualLimit, sortBy, noteType, publishTime);
    }

    private TopicAnalysisVO searchByMcp(String keyword, int actualLimit, String sortBy, String noteType,
            String publishTime) {
        if (!mcpClientService.hasTool(McpRegistry.Tools.XHS_SEARCH_FEEDS)) {
            throw new IllegalStateException("MCP tool not found: " + McpRegistry.Tools.XHS_SEARCH_FEEDS);
        }

        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("keyword", keyword);
        Map<String, Object> filters = new LinkedHashMap<>();
        putIfPresent(filters, "sort_by", sortBy);
        putIfPresent(filters, "note_type", noteType);
        putIfPresent(filters, "publish_time", publishTime);
        if (!filters.isEmpty()) {
            arguments.put("filters", filters);
        }

        McpSchema.CallToolResult searchResult = mcpClientService.callTool(McpRegistry.Tools.XHS_SEARCH_FEEDS, arguments);
        if (Boolean.TRUE.equals(searchResult.isError())) {
            throw new IllegalStateException("小红书搜索失败，请确认账号已登录并且 MCP 服务可用");
        }
        String rawSearch = mcpClientService.toJson(searchResult);
        List<TopicAnalysisVO.FeedSample> samples = parseSamples(rawSearch, actualLimit);
        samples = enrichDetails(samples);
        SummaryResult summary = summarize(keyword, samples);
        return new TopicAnalysisVO(keyword, samples, summary.content(), summary.recommendations(), rawSearch,
                summary.errorMessage());
    }

    List<TopicAnalysisVO.FeedSample> parseSamples(String raw, int limit) {
        List<JsonNode> candidates = new ArrayList<>();
        collectCandidateObjects(parseJson(raw), candidates);
        List<TopicAnalysisVO.FeedSample> samples = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode node : candidates) {
            TopicAnalysisVO.FeedSample sample = toSample(node, null);
            String key = firstText(sample.feedId(), sample.url(), sample.title());
            if (key == null || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            samples.add(sample);
            if (samples.size() >= limit) {
                break;
            }
        }
        return samples;
    }

    private List<TopicAnalysisVO.FeedSample> enrichDetails(List<TopicAnalysisVO.FeedSample> samples) {
        if (!mcpClientService.hasTool(McpRegistry.Tools.XHS_GET_FEED_DETAIL)) {
            return samples;
        }
        List<TopicAnalysisVO.FeedSample> enriched = new ArrayList<>();
        for (TopicAnalysisVO.FeedSample sample : samples) {
            if (isBlank(sample.feedId()) || isBlank(sample.xsecToken())) {
                enriched.add(sample);
                continue;
            }
            try {
                Map<String, Object> arguments = new LinkedHashMap<>();
                arguments.put("feed_id", sample.feedId());
                arguments.put("xsec_token", sample.xsecToken());
                arguments.put("load_all_comments", false);
                McpSchema.CallToolResult detailResult = mcpClientService.callTool(McpRegistry.Tools.XHS_GET_FEED_DETAIL,
                        arguments);
                String rawDetail = mcpClientService.toJson(detailResult);
                TopicAnalysisVO.FeedSample detail = parseSamples(rawDetail, 1).stream()
                        .findFirst()
                        .orElse(toSample(parseJson(rawDetail), rawDetail));
                enriched.add(merge(sample, detail, rawDetail));
            }
            catch (Exception ex) {
                enriched.add(sample);
            }
        }
        return enriched;
    }

    private SummaryResult summarize(String keyword, List<TopicAnalysisVO.FeedSample> samples) {
        if (samples.isEmpty()) {
            return new SummaryResult("未搜索到可分析的笔记样本。", List.of(), null);
        }
        String prompt = """
                请基于下面的小红书热门笔记样本，做选题分析。
                关键词：%s
                请输出中文，包含：热门角度、标题套路、用户痛点、内容机会、推荐选题。
                推荐选题请尽量用 JSON 数组字段 recommendations 表达，每项包含 title、keywords、audience、direction。

                样本：
                %s
                """.formatted(keyword, samplesForPrompt(samples));
        try {
            String content = chatService.chat(prompt);
            return new SummaryResult(content, parseRecommendations(content), null);
        }
        catch (Exception ex) {
            return new SummaryResult(null, fallbackRecommendations(keyword, samples), ex.getMessage());
        }
    }

    private String samplesForPrompt(List<TopicAnalysisVO.FeedSample> samples) {
        return samples.stream()
                .limit(10)
                .map(sample -> "- 标题：" + nullToEmpty(sample.title())
                        + "\n  作者：" + nullToEmpty(sample.author())
                        + "\n  互动：点赞" + nullToDash(sample.likes())
                        + " 收藏" + nullToDash(sample.collects())
                        + " 评论" + nullToDash(sample.comments())
                        + "\n  正文：" + truncate(sample.content(), 260)
                        + "\n  评论样本：" + String.join(" / ", sample.commentSamples()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private List<TopicAnalysisVO.TopicRecommendation> parseRecommendations(String content) {
        JsonNode root = parseJson(content);
        JsonNode recommendations = findField(root, "recommendations");
        if (recommendations == null || !recommendations.isArray()) {
            return List.of();
        }
        List<TopicAnalysisVO.TopicRecommendation> result = new ArrayList<>();
        for (JsonNode item : recommendations) {
            String title = findText(item, "title", "选题", "topic");
            if (isBlank(title)) {
                continue;
            }
            result.add(new TopicAnalysisVO.TopicRecommendation(
                    title,
                    findText(item, "keywords", "关键词"),
                    findText(item, "audience", "目标人群"),
                    findText(item, "direction", "内容方向", "angle")));
        }
        return result;
    }

    private List<TopicAnalysisVO.TopicRecommendation> fallbackRecommendations(String keyword,
            List<TopicAnalysisVO.FeedSample> samples) {
        return samples.stream()
                .map(TopicAnalysisVO.FeedSample::title)
                .filter(value -> !isBlank(value))
                .limit(5)
                .map(title -> new TopicAnalysisVO.TopicRecommendation(title, keyword, null, "参考热门笔记角度进行二次创作"))
                .toList();
    }

    private TopicAnalysisVO.FeedSample toSample(JsonNode node, String rawDetail) {
        String feedId = findText(node, "feed_id", "feedId", "note_id", "noteId", "id");
        String xsecToken = findText(node, "xsec_token", "xsecToken");
        String title = findText(node, "title", "display_title", "displayTitle", "note_title");
        String content = findText(node, "content", "desc", "description", "text");
        String author = findText(node, "nickname", "nickName", "userName", "username", "author");
        String url = firstText(findText(node, "url", "link", "noteUrl", "note_url", "shareUrl", "share_url"),
                feedId == null ? null : "https://www.xiaohongshu.com/explore/" + feedId);
        return new TopicAnalysisVO.FeedSample(
                feedId,
                xsecToken,
                title,
                content,
                author,
                url,
                findInt(node, "likes", "liked_count", "like_count", "likedCount"),
                findInt(node, "collects", "collected_count", "collect_count", "collectedCount"),
                findInt(node, "comments", "comment_count", "comments_count", "commentCount"),
                findInt(node, "shares", "share_count", "shareCount"),
                collectComments(node, 5),
                rawDetail);
    }

    private TopicAnalysisVO.FeedSample merge(TopicAnalysisVO.FeedSample base, TopicAnalysisVO.FeedSample detail,
            String rawDetail) {
        return new TopicAnalysisVO.FeedSample(
                firstText(base.feedId(), detail.feedId()),
                firstText(base.xsecToken(), detail.xsecToken()),
                firstText(detail.title(), base.title()),
                firstText(detail.content(), base.content()),
                firstText(detail.author(), base.author()),
                firstText(detail.url(), base.url()),
                firstInt(detail.likes(), base.likes()),
                firstInt(detail.collects(), base.collects()),
                firstInt(detail.comments(), base.comments()),
                firstInt(detail.shares(), base.shares()),
                detail.commentSamples().isEmpty() ? base.commentSamples() : detail.commentSamples(),
                rawDetail);
    }

    private void collectCandidateObjects(JsonNode node, List<JsonNode> candidates) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            if (isFeedLike(node)) {
                candidates.add(node);
            }
            node.fields().forEachRemaining(field -> collectCandidateObjects(field.getValue(), candidates));
        }
        else if (node.isArray()) {
            for (JsonNode child : node) {
                collectCandidateObjects(child, candidates);
            }
        }
        else if (node.isTextual()) {
            JsonNode nested = parseJson(node.asText());
            if (nested != null) {
                collectCandidateObjects(nested, candidates);
            }
        }
    }

    private boolean isFeedLike(JsonNode node) {
        boolean hasIdentity = findDirectText(node, "feed_id", "feedId", "note_id", "noteId", "id") != null
                || findDirectText(node, "xsec_token", "xsecToken") != null
                || findDirectText(node, "url", "link", "noteUrl", "note_url") != null;
        boolean hasContent = findDirectText(node, "title", "display_title", "displayTitle", "content", "desc",
                "description") != null;
        return hasIdentity && hasContent;
    }

    private List<String> collectComments(JsonNode node, int limit) {
        List<String> comments = new ArrayList<>();
        collectCommentText(node, comments, limit);
        return comments;
    }

    private void collectCommentText(JsonNode node, List<String> comments, int limit) {
        if (node == null || comments.size() >= limit) {
            return;
        }
        if (node.isObject()) {
            String comment = findDirectText(node, "comment_content", "content", "text");
            if (!isBlank(comment) && comment.length() <= 280 && !comments.contains(comment)) {
                comments.add(comment);
            }
            node.fields().forEachRemaining(field -> {
                if (comments.size() < limit && isCommentContainer(field.getKey())) {
                    collectCommentText(field.getValue(), comments, limit);
                }
            });
        }
        else if (node.isArray()) {
            for (JsonNode child : node) {
                collectCommentText(child, comments, limit);
            }
        }
    }

    private boolean isCommentContainer(String key) {
        String lower = key.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("comment") || lower.contains("reply");
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = extractJson(value.strip());
        if (trimmed == null) {
            return null;
        }
        try {
            return objectMapper.readTree(trimmed);
        }
        catch (Exception ex) {
            return null;
        }
    }

    private String extractJson(String value) {
        if (value.startsWith("{") || value.startsWith("[")) {
            return value;
        }
        int objectStart = value.indexOf('{');
        int arrayStart = value.indexOf('[');
        int start = objectStart < 0 ? arrayStart : arrayStart < 0 ? objectStart : Math.min(objectStart, arrayStart);
        if (start < 0) {
            return null;
        }
        int objectEnd = value.lastIndexOf('}');
        int arrayEnd = value.lastIndexOf(']');
        int end = Math.max(objectEnd, arrayEnd);
        return end > start ? value.substring(start, end + 1) : null;
    }

    private JsonNode findField(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            JsonNode direct = node.get(fieldName);
            if (direct != null) {
                return direct;
            }
            for (JsonNode child : node) {
                JsonNode value = findField(child, fieldName);
                if (value != null) {
                    return value;
                }
            }
        }
        else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode value = findField(child, fieldName);
                if (value != null) {
                    return value;
                }
            }
        }
        else if (node.isTextual()) {
            return findField(parseJson(node.asText()), fieldName);
        }
        return null;
    }

    private String findText(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        String direct = findDirectText(node, names);
        if (direct != null) {
            return direct;
        }
        if (node.isObject()) {
            for (JsonNode child : node) {
                String value = findText(child, names);
                if (value != null) {
                    return value;
                }
            }
        }
        else if (node.isArray()) {
            for (JsonNode child : node) {
                String value = findText(child, names);
                if (value != null) {
                    return value;
                }
            }
        }
        else if (node.isTextual()) {
            return findText(parseJson(node.asText()), names);
        }
        return null;
    }

    private String findDirectText(JsonNode node, String... names) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isValueNode() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private Integer findInt(JsonNode node, String... names) {
        String text = findText(node, names);
        if (text == null) {
            return null;
        }
        String normalized = text.replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(normalized);
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }

    private void putIfPresent(Map<String, Object> map, String key, String value) {
        if (!isBlank(value) && !"不限".equals(value)) {
            map.put(key, value);
        }
    }

    private String firstText(String... values) {
        return Arrays.stream(values).filter(value -> !isBlank(value)).findFirst().orElse(null);
    }

    private Integer firstInt(Integer first, Integer second) {
        return first == null ? second : first;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String nullToDash(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SummaryResult(
            String content,
            List<TopicAnalysisVO.TopicRecommendation> recommendations,
            String errorMessage) {
    }
}
