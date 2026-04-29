package com.yupi.template.service;

import com.yupi.template.config.EmojiPackConfig;
import com.yupi.template.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EmojiPackServiceTest {

    @InjectMocks
    private EmojiPackService emojiPackService;

    @Mock
    private EmojiPackConfig emojiPackConfig;

    @Test
    @DisplayName("空关键词直接返回 null")
    void testBlankKeywords() {
        assertNull(emojiPackService.searchImage(" "));
    }

    @Test
    @DisplayName("返回正确的策略枚举和降级图")
    void testMethodAndFallback() {
        assertEquals(ImageMethodEnum.EMOJI_PACK, emojiPackService.getMethod());
        assertTrue(emojiPackService.getFallbackImage(1).contains("picsum.photos"));
    }
}
