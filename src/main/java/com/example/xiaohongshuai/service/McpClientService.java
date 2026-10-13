package com.example.xiaohongshuai.service;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;

public interface McpClientService {

    /**
     * 从当前 MCP 连接实时读取工具列表。
     *
     * @return MCP 工具元数据列表
     */
    List<McpSchema.Tool> listTools();

    /**
     * 调用指定 MCP 工具。
     *
     * @param name 工具名称
     * @param arguments 工具参数
     * @return MCP 工具调用结果
     */
    McpSchema.CallToolResult callTool(String name, Map<String, Object> arguments);

    /**
     * 判断当前 MCP 客户端列表中是否存在指定工具。
     *
     * @param name 工具名称
     * @return true 表示可调用
     */
    boolean hasTool(String name);

    /**
     * 将对象序列化为 JSON 字符串，序列化失败时退化为 String.valueOf。
     *
     * @param value 任意对象
     * @return JSON 或字符串表示
     */
    String toJson(Object value);
}
