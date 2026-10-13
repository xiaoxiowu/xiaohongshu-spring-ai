package com.example.xiaohongshuai.service;

import java.util.List;
import java.util.Objects;

import com.example.xiaohongshuai.entity.PublishTask;

public interface PublishTaskService {

    /**
     * 发布指定笔记草稿，并记录 MCP 请求和响应。
     *
     * @param noteDraftId 草稿 ID
     * @param request 发布参数
     * @return 发布任务记录
     */
    PublishTask publish(Long noteDraftId, PublishRequest request);

    /**
     * 查询指定发布任务，不存在时抛出业务异常。
     *
     * @param id 发布任务 ID
     * @return 发布任务详情
     */
    PublishTask getRequired(Long id);

    /**
     * 查询发布任务列表，按创建时间倒序返回。
     *
     * @return 发布任务列表
     */
    List<PublishTask> list();

    public record PublishRequest(
            String type,
            List<String> images,
            List<Long> imageAssetIds,
            String video,
            String scheduleAt,
            String visibility,
            Boolean isOriginal) {
        public PublishRequest {
            type = type == null || type.isBlank() ? "image" : type;
            images = images == null ? null : images.stream()
                    .filter(Objects::nonNull)
                    .filter(value -> !value.isBlank())
                    .toList();
            imageAssetIds = imageAssetIds == null ? null : imageAssetIds.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}
