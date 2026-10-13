package com.example.xiaohongshuai.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.example.xiaohongshuai.config.McpRegistry;
import com.example.xiaohongshuai.entity.NoteDraft;
import com.example.xiaohongshuai.entity.PublishTask;
import com.example.xiaohongshuai.mapper.PublishTaskMapper;
import com.example.xiaohongshuai.service.ImageAssetService;
import com.example.xiaohongshuai.service.McpClientService;
import com.example.xiaohongshuai.service.NoteDraftService;
import com.example.xiaohongshuai.service.PublishTaskService;
import com.example.xiaohongshuai.service.XhsBrowserTaskLock;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PublishTaskServiceImpl implements PublishTaskService {

    private final PublishTaskMapper publishTaskMapper;
    private final NoteDraftService noteDraftService;
    private final McpClientService mcpClientService;
    private final ImageAssetService imageAssetService;
    private final ObjectMapper objectMapper;
    private final XhsBrowserTaskLock xhsBrowserTaskLock;

    @Autowired
    public PublishTaskServiceImpl(PublishTaskMapper publishTaskMapper, NoteDraftService noteDraftService,
            McpClientService mcpClientService, ImageAssetService imageAssetService, ObjectMapper objectMapper,
            XhsBrowserTaskLock xhsBrowserTaskLock) {
        this.publishTaskMapper = publishTaskMapper;
        this.noteDraftService = noteDraftService;
        this.mcpClientService = mcpClientService;
        this.imageAssetService = imageAssetService;
        this.objectMapper = objectMapper;
        this.xhsBrowserTaskLock = xhsBrowserTaskLock;
    }

    PublishTaskServiceImpl(PublishTaskMapper publishTaskMapper, NoteDraftService noteDraftService,
            McpClientService mcpClientService, ImageAssetService imageAssetService, ObjectMapper objectMapper) {
        this(publishTaskMapper, noteDraftService, mcpClientService, imageAssetService, objectMapper, null);
    }

    @Override
    public PublishTask publish(Long noteDraftId, PublishRequest request) {
        XhsBrowserTaskLock.Lease lease = xhsBrowserTaskLock == null ? null : xhsBrowserTaskLock.acquire("发布小红书笔记");
        try {
            NoteDraft draft = noteDraftService.getRequired(noteDraftId);
            PublishRequest actualRequest = request == null
                    ? new PublishRequest(null, null, null, null, null, null, null) : request;
            String toolName = selectToolName(draft, actualRequest);
            List<Long> imageAssetIds = resolveImageAssetIds(draft, actualRequest);
            Map<String, Object> payload = buildPayload(draft, actualRequest);
            LocalDateTime now = LocalDateTime.now();
            PublishTask task = new PublishTask();
            task.setNoteDraftId(noteDraftId);
            task.setStatus("RUNNING");
            task.setRequestPayload(mcpClientService.toJson(buildRequestRecord(toolName, payload, imageAssetIds)));
            task.setStartedAt(now);
            task.setCreatedAt(now);
            task.setUpdatedAt(now);
            publishTaskMapper.insert(task);
            noteDraftService.updatePublishStatus(noteDraftId, "PUBLISHING", draft.getPublishedNoteId());
            Long taskId = task.getId();
            CompletableFuture.runAsync(() -> {
                try {
                    executePublish(taskId, noteDraftId, draft.getPublishedNoteId(), toolName, payload);
                }
                finally {
                    if (lease != null) {
                        lease.close();
                    }
                }
            });
            return task;
        }
        catch (RuntimeException | Error ex) {
            if (lease != null) {
                lease.close();
            }
            throw ex;
        }
    }

    private void executePublish(Long taskId, Long noteDraftId, String publishedNoteId, String toolName,
            Map<String, Object> payload) {
        PublishTask task = publishTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        try {
            McpSchema.CallToolResult result = mcpClientService.callTool(toolName, payload);
            String rawResponse = mcpClientService.toJson(result);
            PublishResult publishResult = parsePublishResult(rawResponse);
            task.setResponsePayload(rawResponse);
            if (Boolean.TRUE.equals(result.isError())) {
                task.setStatus("FAILED");
                task.setErrorMessage(firstText(publishResult.errorMessage(), "MCP tool returned error"));
                noteDraftService.updatePublishStatus(noteDraftId, "FAILED", publishedNoteId);
            }
            else {
                task.setStatus("SUCCESS");
                noteDraftService.updatePublishStatus(noteDraftId, "PUBLISHED",
                        firstText(publishResult.noteId(), publishedNoteId));
            }
        }
        catch (Exception ex) {
            task.setStatus("FAILED");
            task.setErrorMessage(ex.getMessage());
            noteDraftService.updatePublishStatus(noteDraftId, "FAILED", publishedNoteId);
        }

        task.setFinishedAt(LocalDateTime.now());
        task.setUpdatedAt(task.getFinishedAt());
        publishTaskMapper.updateById(task);
    }

    @Override
    public PublishTask getRequired(Long id) {
        PublishTask task = publishTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("Publish task not found: " + id);
        }
        return task;
    }

    @Override
    public List<PublishTask> list() {
        return publishTaskMapper.selectList(new QueryWrapper<PublishTask>().orderByDesc("created_at"));
    }

    private Map<String, Object> buildPayload(NoteDraft draft, PublishRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", draft.getTitle());
        payload.put("content", draft.getContent());
        payload.put("tags", splitTags(draft.getTags()));
        if ("video".equalsIgnoreCase(request.type())) {
            payload.put("video", request.video());
        }
        else {
            List<Long> imageAssetIds = resolveImageAssetIds(draft, request);
            payload.put("images", resolveImages(request, imageAssetIds));
            payload.put("is_original", request.isOriginal());
        }
        if (request.scheduleAt() != null && !request.scheduleAt().isBlank()) {
            payload.put("schedule_at", request.scheduleAt());
        }
        if (request.visibility() != null && !request.visibility().isBlank()) {
            payload.put("visibility", request.visibility());
        }
        return payload;
    }

    private Map<String, Object> buildRequestRecord(String toolName, Map<String, Object> arguments,
            List<Long> imageAssetIds) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("tool", toolName);
        record.put("arguments", arguments);
        if (!imageAssetIds.isEmpty()) {
            record.put("imageAssetIds", imageAssetIds);
        }
        return record;
    }

    private String selectToolName(NoteDraft draft, PublishRequest request) {
        if ("video".equalsIgnoreCase(request.type())) {
            if (request.video() == null || request.video().isBlank()) {
                throw new IllegalArgumentException("video is required when type is video");
            }
            return McpRegistry.Tools.XHS_PUBLISH_WITH_VIDEO;
        }
        if ((request.images() == null || request.images().isEmpty())
                && resolveImageAssetIds(draft, request).isEmpty()) {
            throw new IllegalArgumentException("images or imageAssetIds is required when type is image");
        }
        return McpRegistry.Tools.XHS_PUBLISH_CONTENT;
    }

    private List<String> resolveImages(PublishRequest request, List<Long> imageAssetIds) {
        List<String> manualImages = request.images() == null ? List.of() : request.images();
        List<String> assetImages = imageAssetService.listByIds(imageAssetIds).stream()
                .map(com.example.xiaohongshuai.entity.ImageAsset::getLocalPath)
                .toList();
        if (assetImages.size() != imageAssetIds.size()) {
            throw new IllegalArgumentException("Some image assets were not found");
        }
        return java.util.stream.Stream.concat(manualImages.stream(), assetImages.stream()).toList();
    }

    private List<Long> resolveImageAssetIds(NoteDraft draft, PublishRequest request) {
        if (request.imageAssetIds() != null && !request.imageAssetIds().isEmpty()) {
            return request.imageAssetIds();
        }
        return parseImageAssetIds(draft.getImageAssetIds());
    }

    private List<Long> parseImageAssetIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    PublishResult parsePublishResult(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new PublishResult(null, null, null);
        }
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String url = firstText(findFirstText(root, "noteUrl", "note_url", "url", "shareUrl", "share_url", "link"),
                    findXhsUrl(rawResponse));
            String noteId = firstText(findFirstText(root, "noteId", "note_id", "note_id_str", "publishId",
                    "publish_id", "id"), noteIdFromUrl(url));
            String errorMessage = findFirstText(root, "error", "errorMessage", "error_message", "message", "msg");
            return new PublishResult(noteId, url, errorMessage);
        }
        catch (Exception ex) {
            String url = findXhsUrl(rawResponse);
            return new PublishResult(noteIdFromUrl(url), url, null);
        }
    }

    private String findFirstText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            for (String fieldName : fieldNames) {
                JsonNode value = node.get(fieldName);
                if (value != null && value.isValueNode() && !value.asText().isBlank()) {
                    return value.asText();
                }
            }
            for (JsonNode child : node) {
                String value = findFirstText(child, fieldNames);
                if (value != null) {
                    return value;
                }
            }
        }
        else if (node.isArray()) {
            for (JsonNode child : node) {
                String value = findFirstText(child, fieldNames);
                if (value != null) {
                    return value;
                }
            }
        }
        else if (node.isTextual()) {
            JsonNode nested = readJsonText(node.asText());
            if (nested != null) {
                return findFirstText(nested, fieldNames);
            }
        }
        return null;
    }

    private JsonNode readJsonText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return null;
        }
        try {
            return objectMapper.readTree(trimmed);
        }
        catch (Exception ex) {
            return null;
        }
    }

    private String findXhsUrl(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("https?://[^\\s\"'\\\\]*xiaohongshu\\.com/[^\\s\"'\\\\)]+")
                .matcher(value);
        return matcher.find() ? matcher.group() : null;
    }

    private String noteIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("/(?:explore|discovery/item|item)/([A-Za-z0-9]+)")
                .matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    record PublishResult(String noteId, String noteUrl, String errorMessage) {
    }
}
