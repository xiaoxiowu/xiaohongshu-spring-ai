package com.example.xiaohongshuai.service;

import java.util.List;

import com.example.xiaohongshuai.entity.McpToolSnapshot;

public interface McpToolService {

    /**
     * 实时读取 MCP 工具列表并转换为接口视图。
     *
     * @return MCP 工具视图列表
     */
    List<ToolView> listTools();

    /**
     * 将当前 MCP 工具列表同步保存为数据库快照。
     *
     * @return 已保存的工具快照
     */
    List<McpToolSnapshot> syncTools();

    public record ToolView(
            String name,
            String title,
            String description,
            String inputSchema,
            boolean readOnly,
            boolean destructive) {
    }
}
