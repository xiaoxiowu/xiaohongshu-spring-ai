package com.example.xiaohongshuai.controller;

import java.util.ArrayList;
import java.util.List;

import com.example.common.result.Result;
import com.example.xiaohongshuai.service.McpClientService;
import com.example.xiaohongshuai.vo.SystemConfigVO;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemConfigController {

    private final Environment environment;
    private final McpClientService mcpClientService;

    public SystemConfigController(Environment environment, McpClientService mcpClientService) {
        this.environment = environment;
        this.mcpClientService = mcpClientService;
    }

    @GetMapping("/config")
    public Result<SystemConfigVO> config() {
        return Result.success(new SystemConfigVO(modelConfig(), mcpConfig(), crawlerConfig()));
    }

    private SystemConfigVO.ModelConfig modelConfig() {
        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        return new SystemConfigVO.ModelConfig(
                "OpenAI Compatible",
                environment.getProperty("spring.ai.openai.base-url"),
                environment.getProperty("spring.ai.openai.chat.options.model"),
                environment.getProperty("spring.ai.openai.chat.options.temperature", Double.class),
                apiKey != null && !apiKey.isBlank());
    }

    private SystemConfigVO.McpConfig mcpConfig() {
        Integer toolCount = null;
        String errorMessage = null;
        try {
            toolCount = mcpClientService.listTools().size();
        }
        catch (Exception ex) {
            errorMessage = ex.getMessage();
        }

        return new SystemConfigVO.McpConfig(
                environment.getProperty("spring.ai.mcp.client.enabled", Boolean.class, false),
                environment.getProperty("spring.ai.mcp.client.name"),
                environment.getProperty("spring.ai.mcp.client.type"),
                environment.getProperty("spring.ai.mcp.client.request-timeout"),
                mcpConnections(),
                toolCount,
                errorMessage);
    }

    private List<SystemConfigVO.McpConnection> mcpConnections() {
        List<SystemConfigVO.McpConnection> connections = new ArrayList<>();
        String xhsUrl = environment.getProperty("spring.ai.mcp.client.streamable-http.connections.xiaohongshu.url");
        String xhsEndpoint = environment.getProperty("spring.ai.mcp.client.streamable-http.connections.xiaohongshu.endpoint");
        if (xhsUrl != null || xhsEndpoint != null) {
            connections.add(new SystemConfigVO.McpConnection("xiaohongshu", "streamable-http",
                    xhsUrl, xhsEndpoint, null));
        }

        String stdioConfig = environment.getProperty("spring.ai.mcp.client.stdio.servers-configuration");
        if (stdioConfig != null && !stdioConfig.isBlank()) {
            connections.add(new SystemConfigVO.McpConnection("stdio", "stdio",
                    null, null, stdioConfig));
        }
        return connections;
    }

    private SystemConfigVO.CrawlerConfig crawlerConfig() {
        return new SystemConfigVO.CrawlerConfig(
                environment.getProperty("xhs.topic-analysis.provider"),
                environment.getProperty("xhs.mediacrawler.work-dir"),
                environment.getProperty("xhs.mediacrawler.command"),
                environment.getProperty("xhs.mediacrawler.get-comments", Boolean.class, false),
                environment.getProperty("xhs.mediacrawler.max-comments-per-note", Integer.class));
    }
}
