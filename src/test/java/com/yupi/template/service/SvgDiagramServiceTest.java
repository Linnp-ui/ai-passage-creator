package com.yupi.template.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.yupi.template.config.SvgDiagramConfig;
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
class SvgDiagramServiceTest {

    @InjectMocks
    private SvgDiagramService svgDiagramService;

    @Mock
    private SvgDiagramConfig svgDiagramConfig;

    @Mock
    private DashScopeChatModel chatModel;

    @Test
    @DisplayName("空需求直接返回 null")
    void testBlankRequirement() {
        assertNull(svgDiagramService.getImageData(ImageRequest.builder().prompt(" ").build()));
    }

    @Test
    @DisplayName("返回正确的策略枚举和降级图")
    void testMethodAndFallback() {
        assertEquals(ImageMethodEnum.SVG_DIAGRAM, svgDiagramService.getMethod());
        assertTrue(svgDiagramService.getFallbackImage(1).contains("picsum.photos"));
    }
}
