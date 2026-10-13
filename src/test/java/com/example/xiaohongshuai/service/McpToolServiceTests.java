package com.example.xiaohongshuai.service;

import java.util.List;
import java.util.Map;

import com.example.xiaohongshuai.entity.McpToolSnapshot;
import com.example.xiaohongshuai.mapper.McpToolSnapshotMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.ai.mcp.client.enabled=false",
        "spring.ai.openai.api-key=test"
})
class McpToolServiceTests {

    @MockitoBean
    private McpClientService mcpClientService;

    @Autowired
    private McpToolService mcpToolService;

    @Autowired
    private McpToolSnapshotMapper mcpToolSnapshotMapper;

    @Test
    void syncToolsStoresSnapshot() {
        McpSchema.JsonSchema schema = new McpSchema.JsonSchema("object", Map.of(), List.of(),
                false, null, null);
        McpSchema.ToolAnnotations annotations = new McpSchema.ToolAnnotations("Publish Content",
                false, true, null, null, null);
        McpSchema.Tool tool = new McpSchema.Tool("publish_content", "Publish Content",
                "发布小红书图文内容", schema, null, annotations, null);
        when(mcpClientService.listTools()).thenReturn(List.of(tool));
        when(mcpClientService.toJson(schema)).thenReturn("{\"type\":\"object\"}");

        List<McpToolSnapshot> snapshots = mcpToolService.syncTools();

        assertThat(snapshots).hasSize(1);
        McpToolSnapshot saved = mcpToolSnapshotMapper.selectById(snapshots.get(0).getId());
        assertThat(saved.getName()).isEqualTo("publish_content");
        assertThat(saved.getReadOnly()).isZero();
        assertThat(saved.getDestructive()).isEqualTo(1);
    }
}
