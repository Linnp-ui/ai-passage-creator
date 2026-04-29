package com.yupi.template.service;

import com.yupi.template.model.dto.image.ImageCandidate;
import com.yupi.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AggregatedImageSearchService 集成测试
 */
@ExtendWith(MockitoExtension.class)
class AggregatedImageSearchServiceTest {

    @Mock
    private PexelsService pexelsService;

    @Mock
    private UnsplashService unsplashService;

    @Mock
    private ImageSearchOptimizer imageSearchOptimizer;

    @Mock
    private ImageQualityScorer imageQualityScorer;

    @InjectMocks
    private AggregatedImageSearchService aggregatedService;

    @Nested
    @DisplayName("功能测试")
    class FunctionalTests {

        @Test
        @DisplayName("返回正确的枚举类型")
        void testGetMethod() {
            assertEquals(ImageMethodEnum.AGGREGATED, aggregatedService.getMethod());
        }

        @Test
        @DisplayName("返回降级图片URL")
        void testGetFallbackImage() {
            String fallbackUrl = aggregatedService.getFallbackImage(1);
            assertNotNull(fallbackUrl);
            assertTrue(fallbackUrl.contains("picsum.photos"));
        }

        @Test
        @DisplayName("成功聚合搜索返回最优图片")
        void testSuccessfulAggregatedSearch() {
            when(imageSearchOptimizer.optimizeKeywords(anyString())).thenReturn(List.of("test keyword"));

            List<ImageCandidate> pexelsCandidates = List.of(
                    ImageCandidate.builder()
                            .url("https://pexels.com/image1.jpg")
                            .source("PEXELS")
                            .width(1920)
                            .height(1080)
                            .build()
            );
            when(pexelsService.searchImages(anyString(), anyInt())).thenReturn(pexelsCandidates);
            when(unsplashService.searchImages(anyString(), anyInt())).thenReturn(Collections.emptyList());

            List<ImageCandidate> sortedCandidates = List.of(
                    ImageCandidate.builder()
                            .url("https://pexels.com/image1.jpg")
                            .source("PEXELS")
                            .width(1920)
                            .height(1080)
                            .qualityScore(500.0)
                            .build()
            );
            when(imageQualityScorer.scoreAndSort(anyList())).thenReturn(sortedCandidates);

            String result = aggregatedService.searchImage("test");

            assertNotNull(result);
            verify(imageSearchOptimizer).optimizeKeywords("test");
            verify(pexelsService).searchImages(anyString(), anyInt());
            verify(unsplashService).searchImages(anyString(), anyInt());
            verify(imageQualityScorer).scoreAndSort(anyList());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("空关键词返回null")
        void testEmptyKeywords() {
            ImageCandidate result = aggregatedService.searchBestImage("");
            assertNull(result);
        }

        @Test
        @DisplayName("null关键词返回null")
        void testNullKeywords() {
            ImageCandidate result = aggregatedService.searchBestImage(null);
            assertNull(result);
        }

        @Test
        @DisplayName("纯空格关键词返回null")
        void testWhitespaceOnlyKeywords() {
            ImageCandidate result = aggregatedService.searchBestImage("   ");
            assertNull(result);
        }

        @Test
        @DisplayName("所有图库返回空结果")
        void testAllSourcesReturnEmpty() {
            when(imageSearchOptimizer.optimizeKeywords(anyString())).thenReturn(List.of("test"));
            when(pexelsService.searchImages(anyString(), anyInt())).thenReturn(Collections.emptyList());
            when(unsplashService.searchImages(anyString(), anyInt())).thenReturn(Collections.emptyList());

            String result = aggregatedService.searchImage("nonexistent");

            assertNull(result);
        }

        @Test
        @DisplayName("关键词优化返回空列表")
        void testOptimizerReturnsEmptyList() {
            when(imageSearchOptimizer.optimizeKeywords(anyString())).thenReturn(Collections.emptyList());
            when(pexelsService.searchImages(anyString(), anyInt())).thenReturn(Collections.emptyList());
            when(unsplashService.searchImages(anyString(), anyInt())).thenReturn(Collections.emptyList());

            String result = aggregatedService.searchImage("test");

            assertNull(result);
        }

        @Test
        @DisplayName("关键词优化返回null")
        void testOptimizerReturnsNull() {
            when(imageSearchOptimizer.optimizeKeywords(anyString())).thenReturn(null);
            when(pexelsService.searchImages(anyString(), anyInt())).thenReturn(Collections.emptyList());
            when(unsplashService.searchImages(anyString(), anyInt())).thenReturn(Collections.emptyList());

            String result = aggregatedService.searchImage("test");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Pexels服务异常时继续搜索Unsplash")
        void testPexelsExceptionHandling() {
            when(imageSearchOptimizer.optimizeKeywords(anyString())).thenReturn(List.of("test"));
            when(pexelsService.searchImages(anyString(), anyInt())).thenThrow(new RuntimeException("Pexels API error"));

            List<ImageCandidate> unsplashCandidates = List.of(
                    ImageCandidate.builder()
                            .url("https://unsplash.com/image.jpg")
                            .source("UNSPLASH")
                            .width(1920)
                            .height(1080)
                            .build()
            );
            when(unsplashService.searchImages(anyString(), anyInt())).thenReturn(unsplashCandidates);
            when(imageQualityScorer.scoreAndSort(anyList())).thenReturn(unsplashCandidates);

            String result = aggregatedService.searchImage("test");

            assertNotNull(result);
            assertEquals("https://unsplash.com/image.jpg", result);
        }

        @Test
        @DisplayName("Unsplash服务异常时继续搜索Pexels")
        void testUnsplashExceptionHandling() {
            when(imageSearchOptimizer.optimizeKeywords(anyString())).thenReturn(List.of("test"));

            List<ImageCandidate> pexelsCandidates = List.of(
                    ImageCandidate.builder()
                            .url("https://pexels.com/image.jpg")
                            .source("PEXELS")
                            .width(1920)
                            .height(1080)
                            .build()
            );
            when(pexelsService.searchImages(anyString(), anyInt())).thenReturn(pexelsCandidates);
            when(unsplashService.searchImages(anyString(), anyInt())).thenThrow(new RuntimeException("Unsplash API error"));
            when(imageQualityScorer.scoreAndSort(anyList())).thenReturn(pexelsCandidates);

            String result = aggregatedService.searchImage("test");

            assertNotNull(result);
            assertEquals("https://pexels.com/image.jpg", result);
        }

        @Test
        @DisplayName("评分服务异常")
        void testScorerExceptionHandling() {
            when(imageSearchOptimizer.optimizeKeywords(anyString())).thenReturn(List.of("test"));

            List<ImageCandidate> candidates = List.of(
                    ImageCandidate.builder()
                            .url("https://example.com/image.jpg")
                            .source("PEXELS")
                            .build()
            );
            when(pexelsService.searchImages(anyString(), anyInt())).thenReturn(candidates);
            when(unsplashService.searchImages(anyString(), anyInt())).thenReturn(Collections.emptyList());
            when(imageQualityScorer.scoreAndSort(anyList())).thenThrow(new RuntimeException("Scorer error"));

            assertThrows(RuntimeException.class, () -> aggregatedService.searchImage("test"));
        }
    }

    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("并行搜索多个关键词")
        void testParallelSearch() {
            List<String> optimizedKeywords = List.of("keyword1", "keyword2", "keyword3");
            when(imageSearchOptimizer.optimizeKeywords(anyString())).thenReturn(optimizedKeywords);

            List<ImageCandidate> candidates = List.of(
                    ImageCandidate.builder()
                            .url("https://example.com/image.jpg")
                            .source("PEXELS")
                            .width(1920)
                            .height(1080)
                            .build()
            );
            when(pexelsService.searchImages(anyString(), anyInt())).thenReturn(candidates);
            when(unsplashService.searchImages(anyString(), anyInt())).thenReturn(candidates);
            when(imageQualityScorer.scoreAndSort(anyList())).thenReturn(candidates);

            String result = aggregatedService.searchImage("test");

            assertNotNull(result);
            verify(pexelsService, times(3)).searchImages(anyString(), anyInt());
            verify(unsplashService, times(3)).searchImages(anyString(), anyInt());
        }
    }
}
