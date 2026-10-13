package com.example.xiaohongshuai.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.xiaohongshuai.entity.NoteDraft;
import com.example.xiaohongshuai.entity.Topic;
import com.example.xiaohongshuai.mapper.NoteDraftMapper;
import com.example.xiaohongshuai.service.AiGenerationLogService;
import com.example.xiaohongshuai.service.ChatService;
import com.example.xiaohongshuai.service.NoteDraftService;
import com.example.xiaohongshuai.service.TopicService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteDraftServiceImpl implements NoteDraftService {

    private final NoteDraftMapper noteDraftMapper;
    private final TopicService topicService;
    private final ChatService chatService;
    private final AiGenerationLogService aiGenerationLogService;
    private final ObjectMapper objectMapper;

    public NoteDraftServiceImpl(NoteDraftMapper noteDraftMapper, TopicService topicService,
            ChatService chatService, AiGenerationLogService aiGenerationLogService,
            ObjectMapper objectMapper) {
        this.noteDraftMapper = noteDraftMapper;
        this.topicService = topicService;
        this.chatService = chatService;
        this.aiGenerationLogService = aiGenerationLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public NoteDraft generate(Long topicId, String title, String keywords, String audience, String direction) {
        Topic topic = topicId == null ? null : topicService.getRequired(topicId);
        String prompt = buildPrompt(topic, title, keywords, audience, direction);
        Instant started = Instant.now();
        try {
            String response = chatService.chat(prompt);
            NoteDraft draft = toDraft(topic, response);
            noteDraftMapper.insert(draft);
            if (topic != null) {
                topicService.markGenerated(topic.getId());
            }
            aiGenerationLogService.record("NOTE_GENERATE", draft.getId(), prompt, response, true,
                    null, Duration.between(started, Instant.now()).toMillis());
            return draft;
        }
        catch (RuntimeException ex) {
            aiGenerationLogService.record("NOTE_GENERATE", topicId, prompt, null, false,
                    ex.getMessage(), Duration.between(started, Instant.now()).toMillis());
            throw ex;
        }
    }

    @Override
    public List<NoteDraft> list() {
        return noteDraftMapper.selectList(new QueryWrapper<NoteDraft>().orderByDesc("created_at"));
    }

    @Override
    public NoteDraft getRequired(Long id) {
        NoteDraft draft = noteDraftMapper.selectById(id);
        if (draft == null) {
            throw new IllegalArgumentException("Note draft not found: " + id);
        }
        return draft;
    }

    @Override
    public NoteDraft update(Long id, String title, String content, String tags, String imagePrompt, String status) {
        return update(id, title, content, tags, imagePrompt, null, status);
    }

    @Override
    public NoteDraft update(Long id, String title, String content, String tags, String imagePrompt,
            List<Long> imageAssetIds, String status) {
        NoteDraft draft = getRequired(id);
        if (title != null) {
            draft.setTitle(title);
        }
        if (content != null) {
            draft.setContent(content);
        }
        if (tags != null) {
            draft.setTags(tags);
        }
        if (imagePrompt != null) {
            draft.setImagePrompt(imagePrompt);
        }
        if (imageAssetIds != null) {
            draft.setImageAssetIds(joinImageAssetIds(imageAssetIds));
        }
        if (status != null) {
            draft.setStatus(status);
        }
        draft.setUpdatedAt(LocalDateTime.now());
        noteDraftMapper.updateById(draft);
        return draft;
    }

    @Override
    public void updatePublishStatus(Long id, String status, String publishedNoteId) {
        NoteDraft draft = getRequired(id);
        draft.setStatus(status);
        draft.setPublishedNoteId(publishedNoteId);
        draft.setUpdatedAt(LocalDateTime.now());
        noteDraftMapper.updateById(draft);
    }

    private String buildPrompt(Topic topic, String title, String keywords, String audience, String direction) {
        String actualTitle = topic == null ? title : topic.getTitle();
        String actualKeywords = topic == null ? keywords : topic.getKeywords();
        String actualAudience = topic == null ? audience : topic.getAudience();
        String actualDirection = topic == null ? direction : topic.getDirection();
        return """
                请为小红书生成一篇可发布的笔记草稿，只返回 JSON，不要使用 Markdown 代码块。
                JSON 字段固定为：title、content、tags、imagePrompt。
                选题：%s
                关键词：%s
                目标人群：%s
                内容方向：%s
                """.formatted(nullToEmpty(actualTitle), nullToEmpty(actualKeywords),
                nullToEmpty(actualAudience), nullToEmpty(actualDirection));
    }

    private NoteDraft toDraft(Topic topic, String response) {
        GeneratedNote generated = parseGeneratedNote(response);
        LocalDateTime now = LocalDateTime.now();
        NoteDraft draft = new NoteDraft();
        draft.setTopicId(topic == null ? null : topic.getId());
        draft.setTitle(generated.title());
        draft.setContent(generated.content());
        draft.setTags(generated.tags());
        draft.setImagePrompt(generated.imagePrompt());
        draft.setStatus("DRAFT");
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        return draft;
    }

    private GeneratedNote parseGeneratedNote(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return new GeneratedNote(
                    root.path("title").asText("未命名笔记"),
                    root.path("content").asText(response),
                    root.path("tags").asText(""),
                    root.path("imagePrompt").asText(""));
        }
        catch (Exception ex) {
            String title = response == null || response.isBlank() ? "未命名笔记" : firstLine(response);
            return new GeneratedNote(title, nullToEmpty(response), "", "");
        }
    }

    private String firstLine(String value) {
        int index = value.indexOf('\n');
        String line = index < 0 ? value : value.substring(0, index);
        return line.length() > 200 ? line.substring(0, 200) : line;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String joinImageAssetIds(List<Long> imageAssetIds) {
        return imageAssetIds.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
    }

    private record GeneratedNote(String title, String content, String tags, String imagePrompt) {
    }
}
