package com.example.xiaohongshuai.service;

import java.util.List;

import com.example.xiaohongshuai.entity.NoteDraft;

public interface NoteDraftService {

    /**
     * 根据选题或直接输入调用 AI 生成小红书笔记草稿。
     *
     * @param topicId 选题 ID，可为空
     * @param title 直接生成时的标题
     * @param keywords 直接生成时的关键词
     * @param audience 直接生成时的目标人群
     * @param direction 直接生成时的内容方向
     * @return 已保存的笔记草稿
     */
    NoteDraft generate(Long topicId, String title, String keywords, String audience, String direction);

    /**
     * 查询所有笔记草稿，按创建时间倒序返回。
     *
     * @return 草稿列表
     */
    List<NoteDraft> list();

    /**
     * 查询指定草稿，不存在时抛出业务异常。
     *
     * @param id 草稿 ID
     * @return 草稿详情
     */
    NoteDraft getRequired(Long id);

    /**
     * 修改草稿内容，传入 null 的字段保持不变。
     *
     * @param id 草稿 ID
     * @param title 标题
     * @param content 正文
     * @param tags 标签
     * @param imagePrompt 图片提示词
     * @param status 草稿状态
     * @return 修改后的草稿
     */
    NoteDraft update(Long id, String title, String content, String tags, String imagePrompt, String status);

    /**
     * 修改草稿内容和已关联的图片资源。
     *
     * @param id 草稿 ID
     * @param title 标题
     * @param content 正文
     * @param tags 标签
     * @param imagePrompt 图片提示词
     * @param imageAssetIds 图片资源 ID 列表
     * @param status 草稿状态
     * @return 修改后的草稿
     */
    NoteDraft update(Long id, String title, String content, String tags, String imagePrompt,
            List<Long> imageAssetIds, String status);

    /**
     * 更新草稿发布状态。
     *
     * @param id 草稿 ID
     * @param status 发布状态
     * @param publishedNoteId 小红书笔记 ID，可为空
     */
    void updatePublishStatus(Long id, String status, String publishedNoteId);
}
