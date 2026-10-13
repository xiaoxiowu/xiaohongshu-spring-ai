package com.example.xiaohongshuai.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.xiaohongshuai.entity.McpToolSnapshot;
import com.example.xiaohongshuai.mapper.McpToolSnapshotMapper;
import com.example.xiaohongshuai.service.McpClientService;
import com.example.xiaohongshuai.service.McpToolService;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class McpToolServiceImpl implements McpToolService {

    private final McpClientService mcpClientService;
    private final McpToolSnapshotMapper mcpToolSnapshotMapper;

    public McpToolServiceImpl(McpClientService mcpClientService, McpToolSnapshotMapper mcpToolSnapshotMapper) {
        this.mcpClientService = mcpClientService;
        this.mcpToolSnapshotMapper = mcpToolSnapshotMapper;
    }

    @Override
    public List<ToolView> listTools() {
        return mcpClientService.listTools().stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    public List<McpToolSnapshot> syncTools() {
        LocalDateTime now = LocalDateTime.now();
        mcpToolSnapshotMapper.delete(new QueryWrapper<McpToolSnapshot>().isNotNull("id"));
        List<McpToolSnapshot> snapshots = mcpClientService.listTools().stream()
                .map(tool -> toSnapshot(tool, now))
                .toList();
        for (McpToolSnapshot snapshot : snapshots) {
            mcpToolSnapshotMapper.insert(snapshot);
        }
        return snapshots;
    }

    private ToolView toView(McpSchema.Tool tool) {
        McpSchema.ToolAnnotations annotations = tool.annotations();
        return new ToolView(
                tool.name(),
                annotations == null ? tool.title() : annotations.title(),
                tool.description(),
                mcpClientService.toJson(tool.inputSchema()),
                annotations != null && Boolean.TRUE.equals(annotations.readOnlyHint()),
                annotations != null && Boolean.TRUE.equals(annotations.destructiveHint()));
    }

    private McpToolSnapshot toSnapshot(McpSchema.Tool tool, LocalDateTime now) {
        ToolView view = toView(tool);
        McpToolSnapshot snapshot = new McpToolSnapshot();
        snapshot.setName(view.name());
        snapshot.setTitle(view.title());
        snapshot.setDescription(view.description());
        snapshot.setInputSchema(view.inputSchema());
        snapshot.setReadOnly(view.readOnly() ? 1 : 0);
        snapshot.setDestructive(view.destructive() ? 1 : 0);
        snapshot.setCreatedAt(now);
        snapshot.setUpdatedAt(now);
        return snapshot;
    }
}
