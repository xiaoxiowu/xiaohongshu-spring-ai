package com.example.xiaohongshuai.service;

import java.util.List;

import com.example.xiaohongshuai.entity.ImageAsset;
import org.springframework.core.io.Resource;

public interface ImageAssetService {

    /**
     * 调用即梦 MCP 生成图片并保存到本地资源库。
     *
     * @param request 生成参数
     * @return 保存后的图片资源
     */
    ImageAsset generate(GenerateImageRequest request);

    /**
     * 导入外部图片 URL 并保存本地副本。
     *
     * @param request 导入参数
     * @return 保存后的图片资源
     */
    ImageAsset importUrl(ImportImageUrlRequest request);

    /**
     * 查询图片资源列表。
     *
     * @param keyword 名称、提示词或备注关键词
     * @param tag 标签关键词
     * @param source 来源
     * @param status 状态
     * @return 图片资源列表
     */
    List<ImageAsset> list(String keyword, String tag, String source, String status);

    /**
     * 查询指定图片资源，不存在时抛出业务异常。
     *
     * @param id 图片资源 ID
     * @return 图片资源
     */
    ImageAsset getRequired(Long id);

    /**
     * 批量查询图片资源。
     *
     * @param ids 图片资源 ID 列表
     * @return 图片资源列表
     */
    List<ImageAsset> listByIds(List<Long> ids);

    /**
     * 修改图片资源运营属性。
     *
     * @param id 图片资源 ID
     * @param request 修改参数
     * @return 修改后的图片资源
     */
    ImageAsset update(Long id, UpdateImageAssetRequest request);

    /**
     * 读取本地图片文件。
     *
     * @param id 图片资源 ID
     * @return 图片文件资源
     */
    Resource file(Long id);

    public record GenerateImageRequest(
            String name,
            String prompt,
            String tags,
            String usageScene,
            String aspectRatio,
            String remark) {
    }

    public record ImportImageUrlRequest(
            String name,
            String url,
            String prompt,
            String tags,
            String usageScene,
            String aspectRatio,
            String remark) {
    }

    public record UpdateImageAssetRequest(
            String name,
            String tags,
            String usageScene,
            String aspectRatio,
            String status,
            String remark) {
    }
}
