package com.yupi.template.service;

import com.yupi.template.config.IconifyConfig;
import com.yupi.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IconifyServiceTest {

    @InjectMocks
    private IconifyService iconifyService;

    @Mock
    private IconifyConfig iconifyConfig;

    @Test
    @DisplayName("空关键词直接返回 null")
    void testBlankKeywords() {
        assertNull(iconifyService.searchImage(" "));
    }

    @Test
    @DisplayName("返回正确的策略枚举和降级图")
    void testMethodAndFallback() {
        assertEquals(ImageMethodEnum.ICONIFY, iconifyService.getMethod());
        assertTrue(iconifyService.getFallbackImage(1).contains("picsum.photos"));
    }
}
