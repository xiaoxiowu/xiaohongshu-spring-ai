package com.example.xiaohongshuai.service;

import com.example.xiaohongshuai.entity.AiGenerationLog;
import com.example.xiaohongshuai.entity.ImageAsset;
import com.example.xiaohongshuai.entity.NoteDraft;
import com.example.xiaohongshuai.entity.PublishTask;
import com.example.xiaohongshuai.entity.Topic;
import com.example.xiaohongshuai.mapper.AiGenerationLogMapper;
import com.example.xiaohongshuai.mapper.ImageAssetMapper;
import com.example.xiaohongshuai.mapper.NoteDraftMapper;
import com.example.xiaohongshuai.mapper.PublishTaskMapper;
import com.example.xiaohongshuai.mapper.XhsAccountMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.ai.mcp.client.enabled=false",
        "spring.ai.openai.api-key=test"
})
class DatabaseWorkflowTests {

    @MockitoBean
    private ChatService chatService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private NoteDraftService noteDraftService;

    @Autowired
    private AiGenerationLogService aiGenerationLogService;

    @Autowired
    private PublishTaskService publishTaskService;

    @Autowired
    private NoteDraftMapper noteDraftMapper;

    @Autowired
    private AiGenerationLogMapper aiGenerationLogMapper;

    @Autowired
    private PublishTaskMapper publishTaskMapper;

    @Autowired
    private ImageAssetMapper imageAssetMapper;

    @Autowired
    private XhsAccountService xhsAccountService;

    @Autowired
    private XhsAccountMapper xhsAccountMapper;

    @Test
    void createsTopicAndGeneratesNoteDraftWithAiLog() {
        when(chatService.chat(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("""
                        {"title":"通勤穿搭灵感","content":"今天分享一套适合上班的干净穿搭。","tags":"通勤穿搭,小个子穿搭","imagePrompt":"白色衬衫和牛仔裤平铺图"}
                        """);

        Topic topic = topicService.create("通勤穿搭", "通勤,穿搭", "上班族", "轻松实用");
        NoteDraft draft = noteDraftService.generate(topic.getId(), null, null, null, null);

        assertThat(topic.getId()).isNotNull();
        assertThat(draft.getId()).isNotNull();
        assertThat(draft.getTopicId()).isEqualTo(topic.getId());
        assertThat(draft.getTitle()).isEqualTo("通勤穿搭灵感");
        assertThat(topicService.getRequired(topic.getId()).getStatus()).isEqualTo("GENERATED");

        AiGenerationLog log = aiGenerationLogMapper.selectList(null).get(0);
        assertThat(log.getBizType()).isEqualTo("NOTE_GENERATE");
        assertThat(log.getBizId()).isEqualTo(draft.getId());
        assertThat(log.getSuccess()).isEqualTo(1);
    }

    @Test
    void recordsFailedAiGenerationLog() {
        AiGenerationLog log = aiGenerationLogService.record("NOTE_GENERATE", 99L, "prompt",
                null, false, "boom", 12);

        AiGenerationLog saved = aiGenerationLogMapper.selectById(log.getId());
        assertThat(saved.getSuccess()).isEqualTo(0);
        assertThat(saved.getErrorMessage()).isEqualTo("boom");
    }

    @Test
    void updatesNoteDraftAndStoresFailedPublishTaskWhenMcpUnavailable() throws InterruptedException {
        NoteDraft draft = new NoteDraft();
        draft.setTitle("待发布标题");
        draft.setContent("待发布内容");
        draft.setTags("标签");
        draft.setImagePrompt("图片");
        draft.setStatus("DRAFT");
        draft.setCreatedAt(java.time.LocalDateTime.now());
        draft.setUpdatedAt(draft.getCreatedAt());
        noteDraftMapper.insert(draft);

        NoteDraft updated = noteDraftService.update(draft.getId(), "新标题", "新内容",
                "新标签", "新图片", "READY");
        PublishTask task = publishTaskService.publish(updated.getId(),
                new PublishTaskService.PublishRequest("image", java.util.List.of("https://example.com/a.jpg"),
                        null, null, null, null, null));
        PublishTask finished = waitForFinishedTask(task.getId());

        assertThat(updated.getTitle()).isEqualTo("新标题");
        assertThat(finished.getStatus()).isEqualTo("FAILED");
        assertThat(finished.getErrorMessage()).contains("No MCP client available");
        assertThat(noteDraftService.getRequired(updated.getId()).getStatus()).isEqualTo("FAILED");
        assertThat(publishTaskMapper.selectById(task.getId()).getStatus()).isEqualTo("FAILED");
    }

    @Test
    void validatesPublishMediaBeforeCreatingTask() {
        NoteDraft draft = new NoteDraft();
        draft.setTitle("标题");
        draft.setContent("内容");
        draft.setStatus("DRAFT");
        draft.setCreatedAt(java.time.LocalDateTime.now());
        draft.setUpdatedAt(draft.getCreatedAt());
        noteDraftMapper.insert(draft);

        assertThatThrownBy(() -> publishTaskService.publish(draft.getId(),
                new PublishTaskService.PublishRequest("image", null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("images or imageAssetIds is required");

        assertThatThrownBy(() -> publishTaskService.publish(draft.getId(),
                new PublishTaskService.PublishRequest("video", null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("video is required");
    }

    @Test
    void publishCanUseImageAssetIds() {
        NoteDraft draft = new NoteDraft();
        draft.setTitle("标题");
        draft.setContent("内容");
        draft.setStatus("DRAFT");
        draft.setCreatedAt(java.time.LocalDateTime.now());
        draft.setUpdatedAt(draft.getCreatedAt());
        noteDraftMapper.insert(draft);

        ImageAsset asset = new ImageAsset();
        asset.setName("素材");
        asset.setSource("URL");
        asset.setStatus("ACTIVE");
        asset.setLocalPath("E:\\images\\a.png");
        asset.setCreatedAt(java.time.LocalDateTime.now());
        asset.setUpdatedAt(asset.getCreatedAt());
        imageAssetMapper.insert(asset);

        PublishTask task = publishTaskService.publish(draft.getId(),
                new PublishTaskService.PublishRequest("image", null, java.util.List.of(asset.getId()),
                        null, null, null, null));

        assertThat(task.getRequestPayload()).contains("imageAssetIds");
        assertThat(task.getRequestPayload()).contains("E:\\\\images\\\\a.png");
    }

    @Test
    void accountStatusReturnsUnavailableWhenMcpClientIsMissing() {
        XhsAccountService.AccountStatus status = xhsAccountService.status();

        assertThat(status.mcpOnline()).isFalse();
        assertThat(status.loginStatus()).isEqualTo("MCP_UNAVAILABLE");
        assertThat(xhsAccountMapper.selectById(1L).getLoginStatus()).isEqualTo("MCP_UNAVAILABLE");
    }

    private PublishTask waitForFinishedTask(Long taskId) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            PublishTask task = publishTaskMapper.selectById(taskId);
            if (!"RUNNING".equals(task.getStatus())) {
                return task;
            }
            Thread.sleep(100);
        }
        return publishTaskMapper.selectById(taskId);
    }
}
