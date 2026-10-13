package com.example.xiaohongshuai.service;

import com.example.xiaohongshuai.entity.AiGenerationLog;

public interface AiGenerationLogService {

    /**
     * 记录一次 AI 调用结果，包含业务来源、提示词、响应、错误和耗时。
     *
     * @param bizType 业务类型，例如 NOTE_GENERATE
     * @param bizId 业务记录 ID，可为空
     * @param prompt 请求模型的提示词
     * @param response 模型返回内容，失败时可为空
     * @param success 是否调用成功
     * @param errorMessage 错误信息，成功时为空
     * @param durationMs 调用耗时，单位毫秒
     * @return 已落库的生成记录
     */
    AiGenerationLog record(String bizType, Long bizId, String prompt, String response,
            boolean success, String errorMessage, long durationMs);
}
