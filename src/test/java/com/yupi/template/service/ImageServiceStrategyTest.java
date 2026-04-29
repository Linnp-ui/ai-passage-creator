package com.yupi.template.service;

import com.yupi.template.model.dto.image.ImageData;
import com.yupi.template.model.dto.image.ImageRequest;
import com.yupi.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceStrategyTest {

    private ImageServiceStrategy imageServiceStrategy;

    @Mock
    private ImageSearchService pexelsService;

    @Mock
    private ImageSearchService nanoBananaService;

    @Mock
    private LocalImageStorageService localImageStorageService;

    @BeforeEach
    void setUp() {
        imageServiceStrategy = new ImageServiceStrategy();
        ReflectionTestUtils.setField(imageServiceStrategy, "imageSearchServices", List.of(pexelsService, nanoBananaService));
        ReflectionTestUtils.setField(imageServiceStrategy, "localImageStorageService", localImageStorageService);

        when(pexelsService.getMethod()).thenReturn(ImageMethodEnum.PEXELS);
        when(nanoBananaService.getMethod()).thenReturn(ImageMethodEnum.NANO_BANANA);

        imageServiceStrategy.init();
    }

    @Test
    @DisplayName("非 AI 策略直接返回图库 URL")
    void testGetImageAndUploadForSearchService() {
        ImageRequest request = ImageRequest.builder().keywords("landscape").position(1).build();
        when(pexelsService.isAvailable()).thenReturn(true);
        when(pexelsService.getImage(request)).thenReturn("https://example.com/pexels.jpg");

        ImageServiceStrategy.ImageResult result =
                imageServiceStrategy.getImageAndUpload(ImageMethodEnum.PEXELS.getValue(), request);

        assertTrue(result.isSuccess());
        assertEquals(ImageMethodEnum.PEXELS, result.getMethod());
        assertEquals("https://example.com/pexels.jpg", result.getUrl());
        verify(pexelsService).getImage(request);
        verify(localImageStorageService, never()).saveImage(any(), any(), any());
    }

    @Test
    @DisplayName("AI 策略生成字节数据后上传到本地存储")
    void testGetImageAndUploadForAiService() {
        ImageRequest request = ImageRequest.builder().prompt("draw a chart").position(2).build();
        when(nanoBananaService.isAvailable()).thenReturn(true);
        when(nanoBananaService.getImageData(request)).thenReturn(ImageData.fromBytes(new byte[]{1, 2, 3}, "image/png"));
        when(localImageStorageService.saveImage(any(), eq("image/png"), eq("nano-banana")))
                .thenReturn("http://localhost:8567/api/images/nano-banana/test.png");

        ImageServiceStrategy.ImageResult result =
                imageServiceStrategy.getImageAndUpload(ImageMethodEnum.NANO_BANANA.getValue(), request);

        assertTrue(result.isSuccess());
        assertEquals(ImageMethodEnum.NANO_BANANA, result.getMethod());
        assertEquals("http://localhost:8567/api/images/nano-banana/test.png", result.getUrl());
        verify(nanoBananaService).getImageData(request);
    }

    @Test
    @DisplayName("服务不可用时立即降级")
    void testFallbackWhenServiceUnavailable() {
        ImageRequest request = ImageRequest.builder().keywords("landscape").position(3).build();
        when(pexelsService.isAvailable()).thenReturn(false);
        when(pexelsService.getFallbackImage(3)).thenReturn("https://picsum.photos/800/600?random=3");

        ImageServiceStrategy.ImageResult result =
                imageServiceStrategy.getImageAndUpload(ImageMethodEnum.PEXELS.getValue(), request);

        assertEquals(ImageMethodEnum.PICSUM, result.getMethod());
        assertEquals("https://picsum.photos/800/600?random=3", result.getUrl());
        verify(pexelsService, never()).getImage(any());
    }

    @Test
    @DisplayName("未知策略默认走 Pexels")
    void testUnknownMethodFallsBackToDefaultSearchMethod() {
        ImageRequest request = ImageRequest.builder().keywords("forest").position(1).build();
        when(pexelsService.isAvailable()).thenReturn(true);
        when(pexelsService.getImage(request)).thenReturn("https://example.com/default.jpg");

        ImageServiceStrategy.ImageResult result =
                imageServiceStrategy.getImageAndUpload("UNKNOWN_METHOD", request);

        assertEquals(ImageMethodEnum.PEXELS, result.getMethod());
        assertEquals("https://example.com/default.jpg", result.getUrl());
        verify(pexelsService).getImage(request);
    }

    @Test
    @DisplayName("AI 策略生成失败时降级")
    void testFallbackWhenAiGenerationReturnsNull() {
        ImageRequest request = ImageRequest.builder().prompt("draw a chart").position(4).build();
        when(nanoBananaService.isAvailable()).thenReturn(true);
        when(nanoBananaService.getImageData(request)).thenReturn(null);
        when(pexelsService.getFallbackImage(4)).thenReturn("https://picsum.photos/800/600?random=4");

        ImageServiceStrategy.ImageResult result =
                imageServiceStrategy.getImageAndUpload(ImageMethodEnum.NANO_BANANA.getValue(), request);

        assertEquals(ImageMethodEnum.PICSUM, result.getMethod());
        assertEquals("https://picsum.photos/800/600?random=4", result.getUrl());
    }
}
