package com.example.xiaohongshuai.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.xiaohongshuai.entity.ImageAsset;
import com.example.xiaohongshuai.entity.NoteDraft;
import com.example.xiaohongshuai.entity.PublishTask;
import com.example.xiaohongshuai.vo.ChatVO;
import com.example.xiaohongshuai.vo.ImageAssetVO;
import com.example.xiaohongshuai.vo.NoteDraftVO;
import com.example.xiaohongshuai.vo.PublishTaskVO;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {

    private static final Pattern FIRST_NUMBER = Pattern.compile("\\d+");

    private final ChatService chatService;
    private final NoteDraftService noteDraftService;
    private final PublishTaskService publishTaskService;
    private final XhsAccountService xhsAccountService;
    private final McpToolService mcpToolService;
    private final ImageAssetService imageAssetService;

    public AssistantService(ChatService chatService, NoteDraftService noteDraftService,
            PublishTaskService publishTaskService, XhsAccountService xhsAccountService,
            McpToolService mcpToolService, ImageAssetService imageAssetService) {
        this.chatService = chatService;
        this.noteDraftService = noteDraftService;
        this.publishTaskService = publishTaskService;
        this.xhsAccountService = xhsAccountService;
        this.mcpToolService = mcpToolService;
        this.imageAssetService = imageAssetService;
    }

    public ChatVO handle(String message) {
        String text = message == null ? "" : message.trim();
        try {
            if (containsAny(text, "账号", "登录状态", "扫码状态")) {
                XhsAccountService.AccountStatus status = xhsAccountService.status();
                return new ChatVO("账号状态：" + status.loginStatus()
                        + (status.nickname() == null ? "" : "，昵称：" + status.nickname()),
                        "xhs.account.status", status);
            }
            if (containsAny(text, "同步工具", "刷新工具", "同步 mcp", "同步MCP")) {
                int count = mcpToolService.syncTools().size();
                return new ChatVO("MCP 工具已同步，共 " + count + " 个。", "mcp.tools.sync", count);
            }
            if (containsAny(text, "生成图片", "生图", "图片生成")) {
                String prompt = afterKeyword(text, "生成图片", "生图", "图片生成");
                ImageAsset asset = imageAssetService.generate(new ImageAssetService.GenerateImageRequest(
                        null, fallbackText(prompt, text), null, "AI助手", null, null));
                return new ChatVO("图片已生成并保存到资源库，图片 ID：" + asset.getId(),
                        "image.generate", ImageAssetVO.from(asset));
            }
            if (containsAny(text, "生成草稿", "写草稿", "生成笔记", "写笔记")) {
                String title = afterKeyword(text, "生成草稿", "写草稿", "生成笔记", "写笔记");
                NoteDraft draft = noteDraftService.generate(null, fallbackText(title, text), null, null, null);
                return new ChatVO("草稿已生成，草稿 ID：" + draft.getId() + "，标题：" + draft.getTitle(),
                        "note.generate", NoteDraftVO.from(draft));
            }
            if (containsAny(text, "发布草稿", "发布笔记")) {
                Long noteId = firstNumber(text);
                if (noteId == null) {
                    return new ChatVO("请告诉我要发布的草稿 ID，例如：发布草稿 12。",
                            "note.publish", null);
                }
                PublishTask task = publishTaskService.publish(noteId,
                        new PublishTaskService.PublishRequest("image", List.of(), List.of(),
                                null, null, null, true));
                return new ChatVO("发布任务已创建，任务 ID：" + task.getId(),
                        "note.publish", PublishTaskVO.from(task));
            }
        }
        catch (Exception ex) {
            return new ChatVO("工具调用失败：" + ex.getMessage(), "tool.error", ex.getMessage());
        }
        return new ChatVO(chatService.chat(text));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String afterKeyword(String text, String... keywords) {
        for (String keyword : keywords) {
            int index = text.indexOf(keyword);
            if (index >= 0) {
                return text.substring(index + keyword.length())
                        .replaceFirst("^[：:，,。\\s]+", "")
                        .trim();
            }
        }
        return "";
    }

    private String fallbackText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Long firstNumber(String text) {
        Matcher matcher = FIRST_NUMBER.matcher(text);
        return matcher.find() ? Long.valueOf(matcher.group()) : null;
    }
}
