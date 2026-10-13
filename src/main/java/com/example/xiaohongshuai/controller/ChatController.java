package com.example.xiaohongshuai.controller;

import com.example.common.result.Result;
import com.example.xiaohongshuai.dto.ChatDTO;
import com.example.xiaohongshuai.service.AssistantService;
import com.example.xiaohongshuai.vo.ChatVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AssistantService assistantService;

    public ChatController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping
    public Result<ChatVO> chat(@Valid @RequestBody ChatDTO request) {
        return Result.success(assistantService.handle(request.message()));
    }
}

