package com.yupi.template.service;

import com.yupi.template.config.MermaidConfig;
import com.yupi.template.model.dto.image.ImageRequest;
import com.yupi.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MermaidServiceTest {

    @InjectMocks
    private MermaidService mermaidService;

    @Mock
    private MermaidConfig mermaidConfig;

    @Test
    @DisplayName("空 Mermaid 代码直接返回 null")
    void testBlankMermaidCode() {
        assertNull(mermaidService.getImageData(ImageRequest.builder().prompt(" ").build()));
    }

    @Test
    @DisplayName("返回正确的策略枚举和降级图")
    void testMethodAndFallback() {
        assertEquals(ImageMethodEnum.MERMAID, mermaidService.getMethod());
        assertTrue(mermaidService.getFallbackImage(1).contains("picsum.photos"));
    }
}
