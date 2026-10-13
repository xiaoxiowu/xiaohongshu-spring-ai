package com.example.xiaohongshuai.service;

import java.util.List;
import java.util.Map;

import com.example.xiaohongshuai.config.McpRegistry;
import com.example.xiaohongshuai.vo.TopicAnalysisVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopicAnalysisServiceTests {

    private final McpClientService mcpClientService = mock(McpClientService.class);
    private final ChatService chatService = mock(ChatService.class);
    private final TopicAnalysisService service = new TopicAnalysisService(mcpClientService, chatService,
            new ObjectMapper());

    @Test
    void parsesNestedSearchResultAndSummarizes() {
        McpSchema.CallToolResult searchResult = new McpSchema.CallToolResult("ok", false);
        McpSchema.CallToolResult detailResult = new McpSchema.CallToolResult("ok", false);
        when(mcpClientService.hasTool(McpRegistry.Tools.XHS_SEARCH_FEEDS)).thenReturn(true);
        when(mcpClientService.hasTool(McpRegistry.Tools.XHS_GET_FEED_DETAIL)).thenReturn(true);
        when(mcpClientService.callTool(eq(McpRegistry.Tools.XHS_SEARCH_FEEDS), any())).thenReturn(searchResult);
        when(mcpClientService.callTool(eq(McpRegistry.Tools.XHS_GET_FEED_DETAIL), any())).thenReturn(detailResult);
        when(mcpClientService.toJson(searchResult)).thenReturn("""
                {"content":[{"text":"{\\"items\\":[{\\"feed_id\\":\\"abc123\\",\\"xsec_token\\":\\"token\\",\\"title\\":\\"通勤穿搭公式\\",\\"liked_count\\":\\"1200\\"}]}"}]}
                """);
        when(mcpClientService.toJson(detailResult)).thenReturn("""
                {"content":[{"text":"{\\"feed_id\\":\\"abc123\\",\\"content\\":\\"适合上班族的干净穿搭\\",\\"comments\\":[{\\"content\\":\\"想看平价版\\"}]}"}]}
                """);
        when(chatService.chat(any())).thenReturn("""
                {"recommendations":[{"title":"通勤穿搭平价公式","keywords":"通勤穿搭,平价","audience":"上班族","direction":"用热门样本拆解平价搭配"}]}
                """);

        TopicAnalysisVO result = service.search("通勤穿搭", 10, "最多点赞", "图文", "一周内");

        assertThat(result.samples()).hasSize(1);
        assertThat(result.samples().get(0).feedId()).isEqualTo("abc123");
        assertThat(result.samples().get(0).url()).isEqualTo("https://www.xiaohongshu.com/explore/abc123");
        assertThat(result.samples().get(0).commentSamples()).contains("想看平价版");
        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().get(0).title()).isEqualTo("通勤穿搭平价公式");
    }

    @Test
    void keepsSamplesWhenSummaryFails() {
        McpSchema.CallToolResult searchResult = new McpSchema.CallToolResult("ok", false);
        when(mcpClientService.hasTool(McpRegistry.Tools.XHS_SEARCH_FEEDS)).thenReturn(true);
        when(mcpClientService.hasTool(McpRegistry.Tools.XHS_GET_FEED_DETAIL)).thenReturn(false);
        when(mcpClientService.callTool(eq(McpRegistry.Tools.XHS_SEARCH_FEEDS), any())).thenReturn(searchResult);
        when(mcpClientService.toJson(searchResult)).thenReturn("""
                {"items":[{"feed_id":"abc123","title":"通勤穿搭公式"}]}
                """);
        when(chatService.chat(any())).thenThrow(new IllegalStateException("model down"));

        TopicAnalysisVO result = service.search("通勤穿搭", 10, null, null, null);

        assertThat(result.samples()).hasSize(1);
        assertThat(result.summaryError()).isEqualTo("model down");
        assertThat(result.recommendations()).extracting(TopicAnalysisVO.TopicRecommendation::title)
                .contains("通勤穿搭公式");
    }

    @Test
    void parsesSamplesDirectly() {
        List<TopicAnalysisVO.FeedSample> samples = service.parseSamples("""
                {"items":[{"id":"note1","xsecToken":"token","display_title":"标题","url":"https://www.xiaohongshu.com/explore/note1"}]}
                """, 5);

        assertThat(samples).hasSize(1);
        assertThat(samples.get(0).feedId()).isEqualTo("note1");
        assertThat(samples.get(0).title()).isEqualTo("标题");
    }
}
