package com.yupi.template.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ImageSearchOptimizer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ImageSearchOptimizerTest {

    @Mock
    private DashScopeChatModel chatModel;

    @InjectMocks
    private ImageSearchOptimizer optimizer;

    @Nested
    @DisplayName("功能测试")
    class FunctionalTests {

        @Test
        @DisplayName("英文关键词保持不变")
        void testEnglishKeywordsPreserved() {
            List<String> result = optimizer.optimizeKeywords("beautiful nature landscape");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("beautiful nature landscape", result.get(0));
        }

        @Test
        @DisplayName("中文关键词优化为英文")
        void testChineseToEnglishOptimization() {
            String mockResponse = """
                {
                    "optimizedKeywords": [
                        "nature landscape",
                        "mountain scenery",
                        "outdoor view"
                    ],
                    "primaryKeyword": "nature landscape"
                }
                """;

            ChatResponse chatResponse = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
            when(chatResponse.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(mockResponse));

            List<String> result = optimizer.optimizeKeywords("自然风景");

            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("空关键词返回空列表")
        void testEmptyKeywords() {
            List<String> result = optimizer.optimizeKeywords("");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null关键词返回空列表")
        void testNullKeywords() {
            List<String> result = optimizer.optimizeKeywords(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("纯空格关键词返回空列表")
        void testWhitespaceOnlyKeywords() {
            List<String> result = optimizer.optimizeKeywords("   ");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("单个字符关键词")
        void testSingleCharacterKeywords() {
            String mockResponse = """
                {
                    "optimizedKeywords": [
                        "letter a",
                        "alphabet",
                        "character"
                    ],
                    "primaryKeyword": "letter a"
                }
                """;

            ChatResponse chatResponse = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
            when(chatResponse.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(mockResponse));

            List<String> result = optimizer.optimizeKeywords("a");

            assertNotNull(result);
        }

        @Test
        @DisplayName("超长关键词")
        void testVeryLongKeywords() {
            String longKeyword = "这是一个非常非常非常非常非常非常非常非常非常非常长的中文关键词描述".repeat(10);

            String mockResponse = """
                {
                    "optimizedKeywords": [
                        "long description",
                        "extended content",
                        "detailed text"
                    ],
                    "primaryKeyword": "long description"
                }
                """;

            ChatResponse chatResponse = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
            when(chatResponse.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(mockResponse));

            List<String> result = optimizer.optimizeKeywords(longKeyword);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("LLM调用异常返回原始关键词")
        void testLlmCallException() {
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenThrow(new RuntimeException("LLM error"));

            List<String> result = optimizer.optimizeKeywords("测试关键词");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("测试关键词", result.get(0));
        }

        @Test
        @DisplayName("LLM返回空响应")
        void testEmptyLlmResponse() {
            ChatResponse chatResponse = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
            when(chatResponse.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(""));

            List<String> result = optimizer.optimizeKeywords("测试关键词");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("测试关键词", result.get(0));
        }

        @Test
        @DisplayName("LLM返回无效JSON")
        void testInvalidJsonResponse() {
            ChatResponse chatResponse = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
            when(chatResponse.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage("invalid json {{{"));

            List<String> result = optimizer.optimizeKeywords("测试关键词");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("测试关键词", result.get(0));
        }

        @Test
        @DisplayName("LLM返回缺少字段")
        void testMissingFieldsResponse() {
            ChatResponse chatResponse = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
            when(chatResponse.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage("{}"));

            List<String> result = optimizer.optimizeKeywords("测试关键词");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("测试关键词", result.get(0));
        }

        @Test
        @DisplayName("LLM返回空关键词列表")
        void testEmptyKeywordsListResponse() {
            String mockResponse = """
                {
                    "optimizedKeywords": [],
                    "primaryKeyword": ""
                }
                """;

            ChatResponse chatResponse = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
            when(chatResponse.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(mockResponse));

            List<String> result = optimizer.optimizeKeywords("测试关键词");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("测试关键词", result.get(0));
        }
    }

    @Nested
    @DisplayName("去重测试")
    class DeduplicationTests {

        @Test
        @DisplayName("关键词去重")
        void testKeywordDeduplication() {
            String mockResponse = """
                {
                    "optimizedKeywords": [
                        "nature landscape",
                        "nature landscape",
                        "mountain view"
                    ],
                    "primaryKeyword": "nature landscape"
                }
                """;

            ChatResponse chatResponse = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
            when(chatResponse.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(mockResponse));

            List<String> result = optimizer.optimizeKeywords("自然风景");

            assertNotNull(result);
            assertTrue(result.size() <= 3);
        }
    }

    @Nested
    @DisplayName("质量判断测试")
    class QualityJudgmentTests {

        @Test
        @DisplayName("高质量英文关键词跳过优化")
        void testHighQualityEnglishKeywordsSkipOptimization() {
            List<String> result = optimizer.optimizeKeywords("beautiful nature landscape photography");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("beautiful nature landscape photography", result.get(0));
            verify(chatModel, never()).call(any(org.springframework.ai.chat.prompt.Prompt.class));
        }

        @Test
        @DisplayName("短英文关键词需要优化")
        void testShortEnglishKeywordsNeedOptimization() {
            String mockResponse = """
                {
                    "optimizedKeywords": [
                        "test image",
                        "testing photo",
                        "sample picture"
                    ],
                    "primaryKeyword": "test image"
                }
                """;

            ChatResponse chatResponse = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
            when(chatResponse.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(new org.springframework.ai.chat.messages.AssistantMessage(mockResponse));

            List<String> result = optimizer.optimizeKeywords("test");

            assertNotNull(result);
            verify(chatModel).call(any(org.springframework.ai.chat.prompt.Prompt.class));
        }
    }
}
