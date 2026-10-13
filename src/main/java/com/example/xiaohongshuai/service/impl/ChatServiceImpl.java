package com.example.xiaohongshuai.service.impl;

import com.example.xiaohongshuai.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String chat(String message) {
        String content = chatClient.prompt()
                .user(message)
                .call()
                .content();

        return content == null ? "" : content;
    }
}
