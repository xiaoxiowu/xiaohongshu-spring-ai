package com.example.xiaohongshuai.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.xiaohongshuai.service.McpClientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class McpClientServiceImpl implements McpClientService {

    private final ObjectProvider<List<McpSyncClient>> mcpClients;
    private final ObjectMapper objectMapper;

    public McpClientServiceImpl(ObjectProvider<List<McpSyncClient>> mcpClients, ObjectMapper objectMapper) {
        this.mcpClients = mcpClients;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<McpSchema.Tool> listTools() {
        List<McpSyncClient> clients = getClients();
        List<McpSchema.Tool> tools = clients.stream()
                .flatMap(client -> safeListTools(client).stream())
                .toList();
        if (tools.isEmpty()) {
            throw new IllegalStateException("No MCP tools available");
        }
        return tools;
    }

    @Override
    public McpSchema.CallToolResult callTool(String name, Map<String, Object> arguments) {
        return findClientByTool(name)
                .orElseThrow(() -> new IllegalArgumentException("MCP tool not found: " + name))
                .callTool(new McpSchema.CallToolRequest(name, arguments));
    }

    @Override
    public boolean hasTool(String name) {
        return findClientByTool(name).isPresent();
    }

    @Override
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private List<McpSyncClient> getClients() {
        List<McpSyncClient> clients = mcpClients.stream()
                .flatMap(List::stream)
                .toList();
        if (clients.isEmpty()) {
            throw new IllegalStateException("No MCP client available. Please make sure MCP servers are configured and running.");
        }
        return clients;
    }

    private Optional<McpSyncClient> findClientByTool(String name) {
        for (McpSyncClient client : getClients()) {
            boolean matched = safeListTools(client).stream()
                    .anyMatch(tool -> name.equals(tool.name()));
            if (matched) {
                return Optional.of(client);
            }
        }
        return Optional.empty();
    }

    private List<McpSchema.Tool> safeListTools(McpSyncClient client) {
        try {
            return client.listTools().tools();
        }
        catch (Exception ex) {
            return List.of();
        }
    }
}
