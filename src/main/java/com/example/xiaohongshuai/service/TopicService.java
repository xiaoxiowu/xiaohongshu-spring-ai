package com.example.xiaohongshuai.service;

import java.util.List;

import com.example.xiaohongshuai.entity.Topic;

public interface TopicService {

    /**
     * 创建一个待生成的小红书选题。
     *
     * @param title 选题标题
     * @param keywords 关键词，逗号分隔
     * @param audience 目标人群
     * @param direction 内容方向
     * @return 已创建的选题
     */
    Topic create(String title, String keywords, String audience, String direction);

    /**
     * 查询选题列表，按创建时间倒序返回。
     *
     * @return 选题列表
     */
    List<Topic> list();

    /**
     * 查询指定选题，不存在时抛出业务异常。
     *
     * @param id 选题 ID
     * @return 选题
     */
    Topic getRequired(Long id);

    /**
     * 将选题标记为已生成草稿。
     *
     * @param id 选题 ID
     */
    void markGenerated(Long id);
}
