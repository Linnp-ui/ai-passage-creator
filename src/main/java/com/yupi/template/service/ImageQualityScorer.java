package com.yupi.template.service;

import com.yupi.template.model.dto.image.ImageCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 图片质量评分服务
 * 对多源返回的图片候选进行质量评分和排序
 *
 * 评分维度：
 * 1. 分辨率得分（像素总数，越高越好）
 * 2. 宽高比得分（越接近 16:9 越好）
 * 3. 来源权重（Unsplash 通常质量更高）
 *
 * @author AI Passage Creator
 */
@Service
@Slf4j
public class ImageQualityScorer {

    /**
     * 目标宽高比（16:9）
     */
    private static final double TARGET_RATIO = 16.0 / 9.0;

    /**
     * 来源质量权重
     */
    private static final double UNSPLASH_WEIGHT = 1.2;
    private static final double PEXELS_WEIGHT = 1.0;

    /**
     * 对图片候选列表进行评分并排序
     *
     * @param candidates 图片候选列表
     * @return 按质量分数降序排序后的列表
     */
    public List<ImageCandidate> scoreAndSort(List<ImageCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // 计算每个候选的得分
        for (ImageCandidate candidate : candidates) {
            double score = calculateScore(candidate);
            candidate.setQualityScore(score);
        }

        // 按得分降序排序
        List<ImageCandidate> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(ImageCandidate::getQualityScore).reversed())
                .toList();

        log.info("图片质量评分完成: total={}, bestScore={}, bestSource={}",
                sorted.size(),
                sorted.get(0).getQualityScore(),
                sorted.get(0).getSource());

        return sorted;
    }

    /**
     * 计算单张图片的质量得分
     *
     * @param candidate 图片候选
     * @return 质量得分（0-1000+）
     */
    private double calculateScore(ImageCandidate candidate) {
        double score = 0;

        // 1. 分辨率得分（像素数 / 100万，最高 500 分）
        long pixelCount = candidate.getPixelCount();
        double resolutionScore = Math.min(pixelCount / 1_000_000.0 * 100, 500);
        score += resolutionScore;

        // 2. 宽高比得分（越接近 16:9 越好，最高 200 分）
        double ratioScore = calculateRatioScore(candidate.getWidth(), candidate.getHeight());
        score += ratioScore;

        // 3. 来源权重加成
        double sourceWeight = getSourceWeight(candidate.getSource());
        score *= sourceWeight;

        return score;
    }

    /**
     * 计算宽高比得分
     */
    private double calculateRatioScore(Integer width, Integer height) {
        if (width == null || height == null || height == 0) {
            return 0;
        }
        double ratio = (double) width / height;
        double diff = Math.abs(ratio - TARGET_RATIO);
        // 差异越小得分越高，差异超过 2 得 0 分
        return Math.max(0, 200 - diff * 100);
    }

    /**
     * 获取来源权重
     */
    private double getSourceWeight(String source) {
        if (source == null) {
            return 1.0;
        }
        return switch (source.toUpperCase()) {
            case "UNSPLASH" -> UNSPLASH_WEIGHT;
            case "PEXELS" -> PEXELS_WEIGHT;
            default -> 1.0;
        };
    }
}
