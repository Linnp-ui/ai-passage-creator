package com.yupi.template.service;

import com.yupi.template.model.dto.image.ImageCandidate;
import com.yupi.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.yupi.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

/**
 * 聚合图片搜索服务
 * 统筹多源图库检索、关键词优化、质量评分，返回最优图片
 *
 * 工作流程：
 * 1. 使用 ImageSearchOptimizer 优化关键词（中文→英文，生成多组同义词）
 * 2. 并行查询 Pexels 和 Unsplash
 * 3. 使用 ImageQualityScorer 对结果评分排序
 * 4. 返回得分最高的图片
 *
 * @author AI Passage Creator
 */
@Service
@Slf4j
public class AggregatedImageSearchService implements ImageSearchService {

    @Resource
    private PexelsService pexelsService;

    @Resource
    private UnsplashService unsplashService;

    @Resource
    private ImageSearchOptimizer imageSearchOptimizer;

    @Resource
    private ImageQualityScorer imageQualityScorer;

    /**
     * 每组关键词从每个源获取的图片数量
     */
    private static final int IMAGES_PER_SOURCE_PER_KEYWORD = 5;

    @Override
    public String searchImage(String keywords) {
        ImageCandidate best = searchBestImage(keywords);
        return best != null ? best.getUrl() : null;
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.AGGREGATED;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }

    /**
     * 聚合搜索最优图片
     *
     * @param keywords 原始关键词
     * @return 最优图片候选，未找到返回 null
     */
    public ImageCandidate searchBestImage(String keywords) {
        log.info("聚合搜索开始: keywords={}", keywords);

        if (keywords == null || keywords.trim().isEmpty()) {
            log.warn("关键词为空，无法搜索");
            return null;
        }

        // 1. 优化关键词（生成多组同义搜索词）
        List<String> optimizedKeywords = imageSearchOptimizer.optimizeKeywords(keywords);
        if (optimizedKeywords == null || optimizedKeywords.isEmpty()) {
            log.warn("关键词优化结果为空，使用原始关键词");
            optimizedKeywords = List.of(keywords);
        }

        // 2. 并行多源搜索
        List<ImageCandidate> allCandidates = searchMultiSource(optimizedKeywords);

        if (allCandidates.isEmpty()) {
            log.warn("聚合搜索未找到任何图片: keywords={}", keywords);
            return null;
        }

        // 3. 质量评分排序
        List<ImageCandidate> sortedCandidates = imageQualityScorer.scoreAndSort(allCandidates);

        ImageCandidate best = sortedCandidates.get(0);
        log.info("聚合搜索完成: keywords={}, totalCandidates={}, bestSource={}, bestScore={}",
                keywords, sortedCandidates.size(), best.getSource(), best.getQualityScore());

        return best;
    }

    /**
     * 多源并行搜索
     *
     * @param keywordsList 多组关键词
     * @return 所有图片候选
     */
    private List<ImageCandidate> searchMultiSource(List<String> keywordsList) {
        CopyOnWriteArrayList<ImageCandidate> allCandidates = new CopyOnWriteArrayList<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String keywords : keywordsList) {
            // Pexels 搜索任务
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    List<ImageCandidate> pexelsCandidates = pexelsService.searchImages(keywords, IMAGES_PER_SOURCE_PER_KEYWORD);
                    allCandidates.addAll(pexelsCandidates);
                } catch (Exception e) {
                    log.error("Pexels 搜索异常: keywords={}", keywords, e);
                }
            }));

            // Unsplash 搜索任务
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    List<ImageCandidate> unsplashCandidates = unsplashService.searchImages(keywords, IMAGES_PER_SOURCE_PER_KEYWORD);
                    allCandidates.addAll(unsplashCandidates);
                } catch (Exception e) {
                    log.error("Unsplash 搜索异常: keywords={}", keywords, e);
                }
            }));
        }

        // 等待所有任务完成（带超时）
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS); // 30秒超时
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("多源图片搜索超时，返回已获取的结果");
        } catch (Exception e) {
            log.error("多源图片搜索异常", e);
        }

        return new ArrayList<>(allCandidates);
    }
}
