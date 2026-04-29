package com.yupi.template.service;

import com.yupi.template.config.PexelsConfig;
import com.yupi.template.model.dto.image.ImageCandidate;
import com.yupi.template.model.enums.ImageMethodEnum;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PexelsService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PexelsServiceTest {

    @InjectMocks
    private PexelsService pexelsService;

    @Mock
    private PexelsConfig pexelsConfig;

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call call;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(pexelsConfig.getApiKey()).thenReturn("test-api-key");
        ReflectionTestUtils.setField(pexelsService, "httpClient", httpClient);
        lenient().when(httpClient.newCall(any())).thenReturn(call);
        lenient().when(call.execute()).thenThrow(new java.io.IOException("test"));
    }

    @Nested
    @DisplayName("功能测试")
    class FunctionalTests {

        @Test
        @DisplayName("返回正确的枚举类型")
        void testGetMethod() {
            assertEquals(ImageMethodEnum.PEXELS, pexelsService.getMethod());
        }

        @Test
        @DisplayName("返回降级图片URL")
        void testGetFallbackImage() {
            String fallbackUrl = pexelsService.getFallbackImage(1);
            assertNotNull(fallbackUrl);
            assertTrue(fallbackUrl.contains("picsum.photos"));
        }

        @Test
        @DisplayName("返回不同位置的降级图片")
        void testGetFallbackImageDifferentPositions() {
            String url1 = pexelsService.getFallbackImage(1);
            String url2 = pexelsService.getFallbackImage(2);
            assertNotNull(url1);
            assertNotNull(url2);
            assertNotEquals(url1, url2);
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("空关键词搜索返回空列表")
        void testEmptyKeywords() {
            List<ImageCandidate> result = pexelsService.searchImages("", 5);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null关键词搜索返回空列表")
        void testNullKeywords() {
            List<ImageCandidate> result = pexelsService.searchImages(null, 5);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("纯空格关键词搜索返回空列表")
        void testWhitespaceOnlyKeywords() {
            List<ImageCandidate> result = pexelsService.searchImages("   ", 5);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("searchImage方法空关键词返回null")
        void testSearchImageEmptyKeywords() {
            String result = pexelsService.searchImage("");
            assertNull(result);
        }

        @Test
        @DisplayName("searchImage方法null关键词返回null")
        void testSearchImageNullKeywords() {
            String result = pexelsService.searchImage(null);
            assertNull(result);
        }

        @Test
        @DisplayName("limit为0时正常处理")
        void testZeroLimit() {
            List<ImageCandidate> result = pexelsService.searchImages("test", 0);
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verifyNoInteractions(httpClient);
        }

        @Test
        @DisplayName("limit为负数时正常处理")
        void testNegativeLimit() {
            List<ImageCandidate> result = pexelsService.searchImages("test", -1);
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verifyNoInteractions(httpClient);
        }
    }

    @Nested
    @DisplayName("安全性测试")
    class SecurityTests {

        @Test
        @DisplayName("SQL注入关键词处理")
        void testSqlInjectionKeywords() {
            List<ImageCandidate> result = pexelsService.searchImages("'; DROP TABLE users; --", 5);
            assertNotNull(result);
        }

        @Test
        @DisplayName("XSS关键词处理")
        void testXssKeywords() {
            List<ImageCandidate> result = pexelsService.searchImages("<script>alert('xss')</script>", 5);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("配置测试")
    class ConfigurationTests {

        @Test
        @DisplayName("验证服务初始化")
        void testServiceInitialization() {
            assertNotNull(pexelsService);
            assertNotNull(pexelsService.getMethod());
        }

        @Test
        @DisplayName("未配置 API Key 时服务不可用")
        void testServiceUnavailableWithoutApiKey() {
            when(pexelsConfig.getApiKey()).thenReturn(" ");
            assertFalse(pexelsService.isAvailable());
        }
    }
}
