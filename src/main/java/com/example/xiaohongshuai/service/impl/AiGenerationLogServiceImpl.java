package com.example.xiaohongshuai.service.impl;

import java.time.LocalDateTime;

import com.example.xiaohongshuai.entity.AiGenerationLog;
import com.example.xiaohongshuai.mapper.AiGenerationLogMapper;
import com.example.xiaohongshuai.service.AiGenerationLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiGenerationLogServiceImpl implements AiGenerationLogService {

    private final AiGenerationLogMapper aiGenerationLogMapper;
    private final String model;

    public AiGenerationLogServiceImpl(AiGenerationLogMapper aiGenerationLogMapper,
            @Value("${spring.ai.openai.chat.options.model:}") String model) {
        this.aiGenerationLogMapper = aiGenerationLogMapper;
        this.model = model;
    }

    @Override
    public AiGenerationLog record(String bizType, Long bizId, String prompt, String response,
            boolean success, String errorMessage, long durationMs) {
        AiGenerationLog log = new AiGenerationLog();
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setModel(model);
        log.setPrompt(prompt);
        log.setResponse(response);
        log.setSuccess(success ? 1 : 0);
        log.setErrorMessage(errorMessage);
        log.setDurationMs(Math.toIntExact(Math.min(durationMs, Integer.MAX_VALUE)));
        log.setCreatedAt(LocalDateTime.now());
        aiGenerationLogMapper.insert(log);
        return log;
    }
}
