package com.yupi.template.model.dto.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImageCandidate 单元测试
 * 测试图片候选 DTO 的各项功能
 */
class ImageCandidateTest {

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("完整构建 ImageCandidate")
        void testFullBuilder() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/image.jpg")
                    .source("PEXELS")
                    .keyword("nature")
                    .width(1920)
                    .height(1080)
                    .photographer("John Doe")
                    .qualityScore(750.5)
                    .rawMetadata("{\"likes\":100}")
                    .build();

            assertNotNull(candidate);
            assertEquals("https://example.com/image.jpg", candidate.getUrl());
            assertEquals("PEXELS", candidate.getSource());
            assertEquals("nature", candidate.getKeyword());
            assertEquals(1920, candidate.getWidth());
            assertEquals(1080, candidate.getHeight());
            assertEquals("John Doe", candidate.getPhotographer());
            assertEquals(750.5, candidate.getQualityScore());
            assertEquals("{\"likes\":100}", candidate.getRawMetadata());
        }

        @Test
        @DisplayName("最小构建 ImageCandidate")
        void testMinimalBuilder() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("https://example.com/image.jpg")
                    .build();

            assertNotNull(candidate);
            assertEquals("https://example.com/image.jpg", candidate.getUrl());
            assertNull(candidate.getSource());
            assertNull(candidate.getWidth());
            assertNull(candidate.getHeight());
        }
    }

    @Nested
    @DisplayName("getPixelCount 测试")
    class PixelCountTests {

        @Test
        @DisplayName("正常分辨率计算像素数")
        void testNormalPixelCount() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(1920)
                    .height(1080)
                    .build();

            long pixelCount = candidate.getPixelCount();

            assertEquals(2_073_600L, pixelCount);
        }

        @Test
        @DisplayName("4K分辨率计算像素数")
        void test4KPixelCount() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(3840)
                    .height(2160)
                    .build();

            long pixelCount = candidate.getPixelCount();

            assertEquals(8_294_400L, pixelCount);
        }

        @Test
        @DisplayName("宽度为null返回0")
        void testNullWidth() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .height(1080)
                    .build();

            long pixelCount = candidate.getPixelCount();

            assertEquals(0L, pixelCount);
        }

        @Test
        @DisplayName("高度为null返回0")
        void testNullHeight() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(1920)
                    .build();

            long pixelCount = candidate.getPixelCount();

            assertEquals(0L, pixelCount);
        }

        @Test
        @DisplayName("宽高都为null返回0")
        void testBothNull() {
            ImageCandidate candidate = ImageCandidate.builder().build();

            long pixelCount = candidate.getPixelCount();

            assertEquals(0L, pixelCount);
        }

        @Test
        @DisplayName("宽度为0返回0")
        void testZeroWidth() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(0)
                    .height(1080)
                    .build();

            long pixelCount = candidate.getPixelCount();

            assertEquals(0L, pixelCount);
        }

        @Test
        @DisplayName("高度为0返回0")
        void testZeroHeight() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(1920)
                    .height(0)
                    .build();

            long pixelCount = candidate.getPixelCount();

            assertEquals(0L, pixelCount);
        }

        @Test
        @DisplayName("极小分辨率计算")
        void testTinyResolution() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(1)
                    .height(1)
                    .build();

            long pixelCount = candidate.getPixelCount();

            assertEquals(1L, pixelCount);
        }

        @Test
        @DisplayName("极大分辨率计算")
        void testHugeResolution() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(100000)
                    .height(100000)
                    .build();

            long pixelCount = candidate.getPixelCount();

            assertEquals(10_000_000_000L, pixelCount);
        }
    }

    @Nested
    @DisplayName("Setter/Getter 测试")
    class SetterGetterTests {

        @Test
        @DisplayName("设置和获取URL")
        void testUrlSetterGetter() {
            ImageCandidate candidate = ImageCandidate.builder().build();
            candidate.setUrl("https://test.com/image.png");
            assertEquals("https://test.com/image.png", candidate.getUrl());
        }

        @Test
        @DisplayName("设置和获取来源")
        void testSourceSetterGetter() {
            ImageCandidate candidate = ImageCandidate.builder().build();
            candidate.setSource("UNSPLASH");
            assertEquals("UNSPLASH", candidate.getSource());
        }

        @Test
        @DisplayName("设置和获取关键词")
        void testKeywordSetterGetter() {
            ImageCandidate candidate = ImageCandidate.builder().build();
            candidate.setKeyword("mountain landscape");
            assertEquals("mountain landscape", candidate.getKeyword());
        }

        @Test
        @DisplayName("设置和获取宽度")
        void testWidthSetterGetter() {
            ImageCandidate candidate = ImageCandidate.builder().build();
            candidate.setWidth(2560);
            assertEquals(2560, candidate.getWidth());
        }

        @Test
        @DisplayName("设置和获取高度")
        void testHeightSetterGetter() {
            ImageCandidate candidate = ImageCandidate.builder().build();
            candidate.setHeight(1440);
            assertEquals(1440, candidate.getHeight());
        }

        @Test
        @DisplayName("设置和获取摄影师")
        void testPhotographerSetterGetter() {
            ImageCandidate candidate = ImageCandidate.builder().build();
            candidate.setPhotographer("Jane Smith");
            assertEquals("Jane Smith", candidate.getPhotographer());
        }

        @Test
        @DisplayName("设置和获取质量评分")
        void testQualityScoreSetterGetter() {
            ImageCandidate candidate = ImageCandidate.builder().build();
            candidate.setQualityScore(999.99);
            assertEquals(999.99, candidate.getQualityScore());
        }

        @Test
        @DisplayName("设置和获取原始元数据")
        void testRawMetadataSetterGetter() {
            ImageCandidate candidate = ImageCandidate.builder().build();
            candidate.setRawMetadata("{\"key\":\"value\"}");
            assertEquals("{\"key\":\"value\"}", candidate.getRawMetadata());
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("null URL处理")
        void testNullUrl() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url(null)
                    .build();

            assertNull(candidate.getUrl());
        }

        @Test
        @DisplayName("空字符串URL处理")
        void testEmptyUrl() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .url("")
                    .build();

            assertEquals("", candidate.getUrl());
        }

        @Test
        @DisplayName("负数宽度处理")
        void testNegativeWidth() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(-100)
                    .height(100)
                    .build();

            assertEquals(-10000L, candidate.getPixelCount());
        }

        @Test
        @DisplayName("负数高度处理")
        void testNegativeHeight() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(100)
                    .height(-100)
                    .build();

            assertEquals(-10000L, candidate.getPixelCount());
        }

        @Test
        @DisplayName("负数质量评分处理")
        void testNegativeQualityScore() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .qualityScore(-100.0)
                    .build();

            assertEquals(-100.0, candidate.getQualityScore());
        }

        @Test
        @DisplayName("极大质量评分处理")
        void testMaxQualityScore() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .qualityScore(Double.MAX_VALUE)
                    .build();

            assertEquals(Double.MAX_VALUE, candidate.getQualityScore());
        }

        @Test
        @DisplayName("极小质量评分处理")
        void testMinQualityScore() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .qualityScore(Double.MIN_VALUE)
                    .build();

            assertEquals(Double.MIN_VALUE, candidate.getQualityScore());
        }
    }

    @Nested
    @DisplayName("数据类型测试")
    class DataTypeTests {

        @Test
        @DisplayName("Integer类型宽高")
        void testIntegerWidthHeight() {
            ImageCandidate candidate = ImageCandidate.builder()
                    .width(Integer.MAX_VALUE)
                    .height(Integer.MAX_VALUE)
                    .build();

            assertEquals(Integer.MAX_VALUE, candidate.getWidth());
            assertEquals(Integer.MAX_VALUE, candidate.getHeight());
            assertTrue(candidate.getPixelCount() > 0);
        }

        @Test
        @DisplayName("Double类型质量评分精度")
        void testDoubleQualityScorePrecision() {
            double preciseScore = 123.456789012345;
            ImageCandidate candidate = ImageCandidate.builder()
                    .qualityScore(preciseScore)
                    .build();

            assertEquals(preciseScore, candidate.getQualityScore(), 0.0000000001);
        }
    }
}
