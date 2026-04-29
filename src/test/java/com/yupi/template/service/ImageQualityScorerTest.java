package com.yupi.template.service;

import com.yupi.template.model.dto.image.ImageCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImageQualityScorer 单元测试
 * 测试图片质量评分服务的各项功能
 */
class ImageQualityScorerTest {

    private ImageQualityScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new ImageQualityScorer();
    }

    @Nested
    @DisplayName("功能测试 - 基本评分功能")
    class BasicScoringTests {

        @Test
        @DisplayName("空列表处理")
        void testEmptyList() {
            List<ImageCandidate> result = scorer.scoreAndSort(List.of());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null列表处理")
        void testNullList() {
            List<ImageCandidate> result = scorer.scoreAndSort(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("单个候选评分")
        void testSingleCandidate() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/image.jpg")
                    .source("PEXELS")
                    .width(1920)
                    .height(1080)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(candidate));

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getQualityScore());
            assertTrue(result.get(0).getQualityScore() > 0);
        }
    }

    @Nested
    @DisplayName("分辨率评分测试")
    class ResolutionScoringTests {

        @Test
        @DisplayName("高分辨率图片得分更高")
        void testHigherResolutionScoresHigher() {
            ImageCandidate lowRes = ImageCandidate.builder()
                    .url("https://example.com/low.jpg")
                    .source("PEXELS")
                    .width(640)
                    .height(480)
                    .build();

            ImageCandidate highRes = ImageCandidate.builder()
                    .url("https://example.com/high.jpg")
                    .source("PEXELS")
                    .width(3840)
                    .height(2160)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(lowRes, highRes));

            assertEquals(2, result.size());
            assertEquals("https://example.com/high.jpg", result.get(0).getUrl());
            assertTrue(result.get(0).getQualityScore() > result.get(1).getQualityScore());
        }

        @Test
        @DisplayName("零分辨率处理")
        void testZeroResolution() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/image.jpg")
                    .source("PEXELS")
                    .width(0)
                    .height(0)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(candidate));

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getQualityScore());
        }

        @Test
        @DisplayName("null分辨率处理")
        void testNullResolution() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/image.jpg")
                    .source("PEXELS")
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(candidate));

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getQualityScore());
        }
    }

    @Nested
    @DisplayName("宽高比评分测试")
    class AspectRatioScoringTests {

        @Test
        @DisplayName("16:9宽高比得分最高")
        void testPerfectAspectRatio() {
            ImageCandidate perfect = ImageCandidate.builder()
                    .url("https://example.com/perfect.jpg")
                    .source("PEXELS")
                    .width(1920)
                    .height(1080)
                    .build();

            ImageCandidate imperfect = ImageCandidate.builder()
                    .url("https://example.com/imperfect.jpg")
                    .source("PEXELS")
                    .width(1000)
                    .height(1000)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(perfect, imperfect));

            assertEquals(2, result.size());
            assertEquals("https://example.com/perfect.jpg", result.get(0).getUrl());
        }

        @Test
        @DisplayName("4:3宽高比得分适中")
        void testModerateAspectRatio() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/4-3.jpg")
                    .source("PEXELS")
                    .width(1600)
                    .height(1200)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(candidate));

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getQualityScore());
        }
    }

    @Nested
    @DisplayName("来源权重测试")
    class SourceWeightTests {

        @Test
        @DisplayName("Unsplash来源权重高于Pexels")
        void testUnsplashWeightHigher() {
            ImageCandidate pexels = ImageCandidate.builder()
                    .url("https://example.com/pexels.jpg")
                    .source("PEXELS")
                    .width(1920)
                    .height(1080)
                    .build();

            ImageCandidate unsplash = ImageCandidate.builder()
                    .url("https://example.com/unsplash.jpg")
                    .source("UNSPLASH")
                    .width(1920)
                    .height(1080)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(pexels, unsplash));

            assertEquals(2, result.size());
            assertEquals("https://example.com/unsplash.jpg", result.get(0).getUrl());
            assertTrue(result.get(0).getQualityScore() > result.get(1).getQualityScore());
        }

        @Test
        @DisplayName("未知来源使用默认权重")
        void testUnknownSourceDefaultWeight() {
            ImageCandidate unknown = ImageCandidate.builder()
                    .url("https://example.com/unknown.jpg")
                    .source("UNKNOWN")
                    .width(1920)
                    .height(1080)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(unknown));

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getQualityScore());
        }

        @Test
        @DisplayName("null来源使用默认权重")
        void testNullSourceDefaultWeight() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/image.jpg")
                    .source(null)
                    .width(1920)
                    .height(1080)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(candidate));

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getQualityScore());
        }
    }

    @Nested
    @DisplayName("排序测试")
    class SortingTests {

        @Test
        @DisplayName("多个候选正确排序")
        void testMultipleCandidatesSorting() {
            List<ImageCandidate> candidates = new ArrayList<>();
            
            candidates.add(ImageCandidate.builder()
                    .url("https://example.com/low.jpg")
                    .source("PEXELS")
                    .width(640)
                    .height(480)
                    .build());
            
            candidates.add(ImageCandidate.builder()
                    .url("https://example.com/high.jpg")
                    .source("UNSPLASH")
                    .width(3840)
                    .height(2160)
                    .build());
            
            candidates.add(ImageCandidate.builder()
                    .url("https://example.com/medium.jpg")
                    .source("PEXELS")
                    .width(1920)
                    .height(1080)
                    .build());

            List<ImageCandidate> result = scorer.scoreAndSort(candidates);

            assertEquals(3, result.size());
            assertEquals("https://example.com/high.jpg", result.get(0).getUrl());
            
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getQualityScore() >= result.get(i + 1).getQualityScore());
            }
        }

        @Test
        @DisplayName("相同分数保持顺序")
        void testSameScoreOrderPreserved() {
            ImageCandidate first = ImageCandidate.builder()
                    .url("https://example.com/first.jpg")
                    .source("PEXELS")
                    .width(1920)
                    .height(1080)
                    .build();

            ImageCandidate second = ImageCandidate.builder()
                    .url("https://example.com/second.jpg")
                    .source("PEXELS")
                    .width(1920)
                    .height(1080)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(first, second));

            assertEquals(2, result.size());
            assertEquals(result.get(0).getQualityScore(), result.get(1).getQualityScore(), 0.001);
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("极大分辨率处理")
        void testExtremeHighResolution() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/extreme.jpg")
                    .source("PEXELS")
                    .width(100000)
                    .height(100000)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(candidate));

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getQualityScore());
            assertTrue(result.get(0).getQualityScore() > 0);
        }

        @Test
        @DisplayName("极小分辨率处理")
        void testExtremeLowResolution() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/tiny.jpg")
                    .source("PEXELS")
                    .width(1)
                    .height(1)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(candidate));

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getQualityScore());
        }

        @Test
        @DisplayName("高度为0时的宽高比计算")
        void testZeroHeightAspectRatio() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/zero-height.jpg")
                    .source("PEXELS")
                    .width(1920)
                    .height(0)
                    .build();

            List<ImageCandidate> result = scorer.scoreAndSort(List.of(candidate));

            assertEquals(1, result.size());
            assertNotNull(result.get(0).getQualityScore());
        }
    }

    @Nested
    @DisplayName("性能测试")
    class PerformanceTests {

        @Test
        @DisplayName("大量候选排序性能")
        void testLargeDatasetPerformance() {
            List<ImageCandidate> candidates = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                candidates.add(ImageCandidate.builder()
                        .url("https://example.com/image" + i + ".jpg")
                        .source(i % 2 == 0 ? "PEXELS" : "UNSPLASH")
                        .width(800 + i)
                        .height(600 + i)
                        .build());
            }

            long startTime = System.currentTimeMillis();
            List<ImageCandidate> result = scorer.scoreAndSort(candidates);
            long endTime = System.currentTimeMillis();

            assertEquals(1000, result.size());
            assertTrue((endTime - startTime) < 1000, "排序1000个候选应在1秒内完成");
            
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getQualityScore() >= result.get(i + 1).getQualityScore());
            }
        }
    }
}
