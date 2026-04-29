package com.yupi.template.service;

import com.yupi.template.config.HuggingFaceConfig;
import com.yupi.template.model.dto.image.ImageRequest;
import com.yupi.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HuggingFaceServiceTest {

    @InjectMocks
    private HuggingFaceService huggingFaceService;

    @Mock
    private HuggingFaceConfig huggingFaceConfig;

    @Test
    @DisplayName("未配置 token 时服务不可用")
    void testUnavailableWithoutToken() {
        when(huggingFaceConfig.getApiToken()).thenReturn("");
        assertFalse(huggingFaceService.isAvailable());
        assertNull(huggingFaceService.getImageData(ImageRequest.builder().prompt("draw").build()));
    }

    @Test
    @DisplayName("返回正确的策略枚举和降级图")
    void testMethodAndFallback() {
        assertEquals(ImageMethodEnum.HUGGINGFACE, huggingFaceService.getMethod());
        assertTrue(huggingFaceService.getFallbackImage(1).contains("picsum.photos"));
    }
}
