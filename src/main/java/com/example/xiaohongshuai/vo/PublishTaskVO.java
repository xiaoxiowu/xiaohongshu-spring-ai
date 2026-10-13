package com.example.xiaohongshuai.vo;

import java.time.LocalDateTime;

import com.example.xiaohongshuai.common.StatusText;
import com.example.xiaohongshuai.entity.PublishTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record PublishTaskVO(
        Long id,
        Long noteDraftId,
        String status,
        String statusText,
        String publishedNoteId,
        String publishedNoteUrl,
        String requestPayload,
        String responsePayload,
        String responseErrorMessage,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static PublishTaskVO from(PublishTask task) {
        ParsedResponse response = parseResponse(task.getResponsePayload());
        return new PublishTaskVO(task.getId(), task.getNoteDraftId(), task.getStatus(),
                StatusText.status(task.getStatus()), response.noteId(), response.noteUrl(),
                task.getRequestPayload(), task.getResponsePayload(), response.errorMessage(), task.getErrorMessage(),
                task.getStartedAt(), task.getFinishedAt(),
                task.getCreatedAt(), task.getUpdatedAt());
    }

    private static ParsedResponse parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new ParsedResponse(null, null, null);
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawResponse);
            String url = firstText(findFirstText(root, "noteUrl", "note_url", "url", "shareUrl", "share_url", "link"),
                    findXhsUrl(rawResponse));
            String noteId = firstText(findFirstText(root, "noteId", "note_id", "note_id_str", "publishId",
                    "publish_id", "id"), noteIdFromUrl(url));
            String errorMessage = findFirstText(root, "error", "errorMessage", "error_message", "message", "msg");
            return new ParsedResponse(noteId, url, errorMessage);
        }
        catch (Exception ex) {
            String url = findXhsUrl(rawResponse);
            return new ParsedResponse(noteIdFromUrl(url), url, null);
        }
    }

    private static String findFirstText(JsonNode node, String... fieldNames) {
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

    private static JsonNode readJsonText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(trimmed);
        }
        catch (Exception ex) {
            return null;
        }
    }

    private static String findXhsUrl(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("https?://[^\\s\"'\\\\]*xiaohongshu\\.com/[^\\s\"'\\\\)]+")
                .matcher(value);
        return matcher.find() ? matcher.group() : null;
    }

    private static String noteIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("/(?:explore|discovery/item|item)/([A-Za-z0-9]+)")
                .matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ParsedResponse(String noteId, String noteUrl, String errorMessage) {
    }
}
