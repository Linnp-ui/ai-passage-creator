package com.yupi.template.model.dto.image;

import lombok.Builder;
import lombok.Data;

/**
 * 图片候选对象
 * 封装图片 URL 及质量评分元数据，用于多源聚合排序
 */
@Data
@Builder
public class ImageCandidate {

    /**
     * 图片 URL
     */
    private String url;

    /**
     * 图片来源
     */
    private String source;

    /**
     * 原始搜索关键词
     */
    private String keyword;

    /**
     * 图片宽度
     */
    private Integer width;

    /**
     * 图片高度
     */
    private Integer height;

    /**
     * 摄影师/作者名
     */
    private String photographer;

    /**
     * 质量评分（综合分数，越高越好）
     */
    private Double qualityScore;

    /**
     * 原始元数据（JSON 字符串，用于扩展）
     */
    private String rawMetadata;

    /**
     * 获取像素总数
     */
    public long getPixelCount() {
        if (width == null || height == null) {
            return 0;
        }
        return (long) width * height;
    }
}
