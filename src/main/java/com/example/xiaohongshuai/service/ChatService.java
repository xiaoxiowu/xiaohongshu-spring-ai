package com.example.xiaohongshuai.service;

public interface ChatService {

    /**
     * 调用当前配置的 AI 模型完成一次普通对话。
     *
     * @param message 用户输入内容
     * @return 模型返回文本，模型无内容时返回空字符串
     */
    String chat(String message);
}

