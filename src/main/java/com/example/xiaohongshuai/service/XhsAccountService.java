package com.example.xiaohongshuai.service;

import io.modelcontextprotocol.spec.McpSchema;

public interface XhsAccountService {

    /**
     * 检查当前小红书 MCP 连接和登录状态，并保存最近一次状态。
     *
     * @return 账号连接状态
     */
    AccountStatus status();

    /**
     * 获取小红书登录二维码。
     *
     * @return MCP 工具返回的二维码结果
     */
    McpSchema.CallToolResult qrcode();

    /**
     * 删除本地 cookies，用于触发重新授权。
     *
     * @return MCP 工具调用结果
     */
    McpSchema.CallToolResult reset();

    public record AccountStatus(
            boolean mcpOnline,
            String loginStatus,
            String nickname,
            String avatarUrl,
            String userId,
            String rawResponse,
            String errorMessage) {
    }
}
