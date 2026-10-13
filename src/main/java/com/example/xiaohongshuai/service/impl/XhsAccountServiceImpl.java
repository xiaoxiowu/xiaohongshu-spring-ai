package com.example.xiaohongshuai.service.impl;

import java.time.LocalDateTime;
import java.util.Map;

import com.example.xiaohongshuai.config.McpRegistry;
import com.example.xiaohongshuai.entity.XhsAccount;
import com.example.xiaohongshuai.mapper.XhsAccountMapper;
import com.example.xiaohongshuai.service.McpClientService;
import com.example.xiaohongshuai.service.XhsBrowserTaskLock;
import com.example.xiaohongshuai.service.XhsAccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XhsAccountServiceImpl implements XhsAccountService {

    private static final Long ACCOUNT_ID = 1L;

    private final McpClientService mcpClientService;
    private final XhsAccountMapper xhsAccountMapper;
    private final ObjectMapper objectMapper;
    private final XhsBrowserTaskLock xhsBrowserTaskLock;

    @Autowired
    public XhsAccountServiceImpl(McpClientService mcpClientService, XhsAccountMapper xhsAccountMapper,
            ObjectMapper objectMapper, XhsBrowserTaskLock xhsBrowserTaskLock) {
        this.mcpClientService = mcpClientService;
        this.xhsAccountMapper = xhsAccountMapper;
        this.objectMapper = objectMapper;
        this.xhsBrowserTaskLock = xhsBrowserTaskLock;
    }

    XhsAccountServiceImpl(McpClientService mcpClientService, XhsAccountMapper xhsAccountMapper,
            ObjectMapper objectMapper) {
        this(mcpClientService, xhsAccountMapper, objectMapper, null);
    }

    @Override
    @Transactional
    public AccountStatus status() {
        if (xhsBrowserTaskLock != null) {
            return xhsBrowserTaskLock.runExclusive("刷新账号状态", this::doStatus);
        }
        return doStatus();
    }

    private AccountStatus doStatus() {
        try {
            McpSchema.CallToolResult result = mcpClientService.callTool(McpRegistry.Tools.XHS_CHECK_LOGIN_STATUS, Map.of());
            String raw = mcpClientService.toJson(result);
            ParsedAccount parsed = parseAccountStatus(raw, Boolean.TRUE.equals(result.isError()));
            saveStatus(parsed.loginStatus(), raw);
            return new AccountStatus(true, parsed.loginStatus(), parsed.nickname(), parsed.avatarUrl(),
                    parsed.userId(), raw, null);
        }
        catch (Exception ex) {
            saveStatus("MCP_UNAVAILABLE", null);
            return new AccountStatus(false, "MCP_UNAVAILABLE", null, null, null, null, ex.getMessage());
        }
    }

    @Override
    public McpSchema.CallToolResult qrcode() {
        if (xhsBrowserTaskLock != null) {
            return xhsBrowserTaskLock.runExclusive("获取登录二维码",
                    () -> mcpClientService.callTool(McpRegistry.Tools.XHS_GET_LOGIN_QRCODE, Map.of()));
        }
        return mcpClientService.callTool(McpRegistry.Tools.XHS_GET_LOGIN_QRCODE, Map.of());
    }

    @Override
    public McpSchema.CallToolResult reset() {
        if (xhsBrowserTaskLock != null) {
            return xhsBrowserTaskLock.runExclusive("重置小红书登录",
                    () -> mcpClientService.callTool(McpRegistry.Tools.XHS_DELETE_COOKIES, Map.of()));
        }
        return mcpClientService.callTool(McpRegistry.Tools.XHS_DELETE_COOKIES, Map.of());
    }

    private void saveStatus(String loginStatus, String raw) {
        LocalDateTime now = LocalDateTime.now();
        XhsAccount account = xhsAccountMapper.selectById(ACCOUNT_ID);
        if (account == null) {
            account = new XhsAccount();
            account.setId(ACCOUNT_ID);
            account.setCreatedAt(now);
        }
        account.setLoginStatus(loginStatus);
        account.setProfileJson(raw);
        account.setLastCheckedAt(now);
        account.setUpdatedAt(now);
        if (account.getId() != null && xhsAccountMapper.selectById(account.getId()) != null) {
            xhsAccountMapper.updateById(account);
        }
        else {
            xhsAccountMapper.insert(account);
        }
    }

    ParsedAccount parseAccountStatus(String raw, boolean isError) {
        if (isError) {
            return new ParsedAccount("ERROR", null, null, null);
        }
        if (raw == null || raw.isBlank()) {
            return new ParsedAccount("UNKNOWN", null, null, null);
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            String nickname = findFirstText(root, "nickname", "nickName", "name", "userName", "username");
            String avatarUrl = findFirstText(root, "avatar", "avatarUrl", "avatar_url", "image", "headUrl", "head_url");
            String userId = findFirstText(root, "userId", "user_id", "uid", "id");
            String loginStatus = inferLoginStatus(root, raw);
            return new ParsedAccount(loginStatus, nickname, avatarUrl, userId);
        }
        catch (Exception ex) {
            return new ParsedAccount(inferLoginStatus(null, raw), null, null, null);
        }
    }

    private String inferLoginStatus(JsonNode root, String raw) {
        Boolean loggedIn = findLoginBoolean(root);
        if (Boolean.TRUE.equals(loggedIn)) {
            return "LOGGED_IN";
        }
        if (Boolean.FALSE.equals(loggedIn)) {
            return "NOT_LOGGED_IN";
        }

        String lower = raw.toLowerCase(java.util.Locale.ROOT);
        if (raw.contains("已登录") || raw.contains("登录成功")
                || lower.contains("logged_in") || lower.contains("login_success")
                || lower.contains("\"isloggedin\":true") || lower.contains("\"islogin\":true")) {
            return "LOGGED_IN";
        }
        if (raw.contains("未登录") || raw.contains("登录过期") || raw.contains("请登录")
                || lower.contains("not_logged") || lower.contains("login_required")
                || lower.contains("not login") || lower.contains("expired")
                || lower.contains("\"isloggedin\":false") || lower.contains("\"islogin\":false")) {
            return "NOT_LOGGED_IN";
        }
        return "UNKNOWN";
    }

    private Boolean findLoginBoolean(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey().toLowerCase(java.util.Locale.ROOT);
                JsonNode value = field.getValue();
                if ((key.contains("login") || key.contains("logged")) && value.isBoolean()) {
                    return value.asBoolean();
                }
                Boolean child = findLoginBoolean(value);
                if (child != null) {
                    return child;
                }
            }
        }
        else if (node.isArray()) {
            for (JsonNode child : node) {
                Boolean value = findLoginBoolean(child);
                if (value != null) {
                    return value;
                }
            }
        }
        else if (node.isTextual()) {
            JsonNode nested = readJsonText(node.asText());
            if (nested != null) {
                return findLoginBoolean(nested);
            }
        }
        return null;
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

    record ParsedAccount(String loginStatus, String nickname, String avatarUrl, String userId) {
    }
}
