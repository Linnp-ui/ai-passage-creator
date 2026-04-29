package com.yupi.template.service;

import com.yupi.template.config.NanoBananaConfig;
import com.yupi.template.model.dto.image.ImageRequest;
import com.yupi.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NanoBananaServiceTest {

    @InjectMocks
    private NanoBananaService nanoBananaService;

    @Mock
    private NanoBananaConfig nanoBananaConfig;

    @Nested
    @DisplayName("配置测试")
    class ConfigurationTests {

        @Test
        @DisplayName("返回正确的枚举类型")
        void testGetMethod() {
            assertEquals(ImageMethodEnum.NANO_BANANA, nanoBananaService.getMethod());
        }

        @Test
        @DisplayName("未配置 API Key 时服务不可用")
        void testUnavailableWithoutApiKey() {
            when(nanoBananaConfig.getApiKey()).thenReturn(" ");
            assertFalse(nanoBananaService.isAvailable());
        }

        @Test
        @DisplayName("配置 API Key 时服务可用")
        void testAvailableWithApiKey() {
            lenient().when(nanoBananaConfig.getApiKey()).thenReturn("test-key");
            assertTrue(nanoBananaService.isAvailable());
        }
    }

    @Test
    @DisplayName("空提示词返回 null")
    void testBlankPromptReturnsNull() {
        ImageRequest request = ImageRequest.builder().prompt("").build();
        assertNull(nanoBananaService.getImageData(request));
    }
}
