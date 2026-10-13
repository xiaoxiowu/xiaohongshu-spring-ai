package com.example.xiaohongshuai.service;

import com.example.xiaohongshuai.entity.NoteDraft;
import com.example.xiaohongshuai.vo.ChatVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantServiceTests {

    private final ChatService chatService = mock(ChatService.class);
    private final NoteDraftService noteDraftService = mock(NoteDraftService.class);
    private final PublishTaskService publishTaskService = mock(PublishTaskService.class);
    private final XhsAccountService xhsAccountService = mock(XhsAccountService.class);
    private final McpToolService mcpToolService = mock(McpToolService.class);
    private final ImageAssetService imageAssetService = mock(ImageAssetService.class);

    private final AssistantService assistantService = new AssistantService(chatService, noteDraftService,
            publishTaskService, xhsAccountService, mcpToolService, imageAssetService);

    @Test
    void generatesNoteDraftWhenRequested() {
        NoteDraft draft = new NoteDraft();
        draft.setId(12L);
        draft.setTitle("通勤穿搭");
        draft.setContent("正文");
        draft.setStatus("DRAFT");

        when(noteDraftService.generate(isNull(), org.mockito.ArgumentMatchers.eq("通勤穿搭"),
                isNull(), isNull(), isNull())).thenReturn(draft);

        ChatVO response = assistantService.handle("生成草稿 通勤穿搭");

        assertThat(response.toolName()).isEqualTo("note.generate");
        assertThat(response.content()).contains("草稿 ID：12");
    }

    @Test
    void fallsBackToChatWhenNoToolIntentMatches() {
        when(chatService.chat("你好")).thenReturn("你好，我在。");

        ChatVO response = assistantService.handle("你好");

        assertThat(response.toolName()).isNull();
        assertThat(response.content()).isEqualTo("你好，我在。");
        verify(chatService).chat("你好");
    }
}
