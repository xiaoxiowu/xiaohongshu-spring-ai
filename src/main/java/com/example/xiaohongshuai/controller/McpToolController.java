package com.example.xiaohongshuai.controller;

import java.util.List;

import com.example.common.result.Result;
import com.example.xiaohongshuai.service.McpToolService;
import com.example.xiaohongshuai.entity.McpToolSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mcp/tools")
public class McpToolController {

    private final McpToolService mcpToolService;

    public McpToolController(McpToolService mcpToolService) {
        this.mcpToolService = mcpToolService;
    }

    @GetMapping
    public Result<List<McpToolService.ToolView>> list() {
        return Result.success(mcpToolService.listTools());
    }

    @PostMapping("/sync")
    public Result<List<McpToolSnapshot>> sync() {
        return Result.success(mcpToolService.syncTools());
    }
}
