package com.yupi.template.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.yupi.template.constant.PromptConstant;
import com.yupi.template.utils.GsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片搜索关键词优化服务
 * 使用 LLM 将中文/模糊关键词翻译并优化为适合图库检索的英文关键词
 * 同时生成多组同义关键词以提高搜索命中率
 *
 * @author AI Passage Creator
 */
@Service
@Slf4j
public class ImageSearchOptimizer {

    @Resource
    private DashScopeChatModel chatModel;

    /**
     * 优化关键词，返回多组搜索词
     *
     * @param originalKeywords 原始关键词（可能是中文）
     * @return 优化后的关键词列表（第一条为最推荐的）
     */
    public List<String> optimizeKeywords(String originalKeywords) {
        if (originalKeywords == null || originalKeywords.trim().isEmpty()) {
            return List.of();
        }

        // 如果已经是纯英文且质量较好，直接返回
        if (isGoodEnglishKeywords(originalKeywords)) {
            log.info("关键词已是高质量英文，跳过优化: {}", originalKeywords);
            return List.of(originalKeywords);
        }

        try {
            String prompt = PromptConstant.IMAGE_KEYWORDS_OPTIMIZATION_PROMPT
                    .replace("{originalKeywords}", originalKeywords);

            ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
            String content = response.getResult().getOutput().getText();

            KeywordOptimizationResult result = GsonUtils.fromJson(content, KeywordOptimizationResult.class);

            if (result == null || result.getOptimizedKeywords() == null || result.getOptimizedKeywords().isEmpty()) {
                log.warn("关键词优化结果为空，使用原始关键词: {}", originalKeywords);
                return List.of(originalKeywords);
            }

            // 去重并限制数量
            List<String> uniqueKeywords = new ArrayList<>();
            for (String kw : result.getOptimizedKeywords()) {
                String trimmed = kw.trim().toLowerCase();
                if (!trimmed.isEmpty() && !uniqueKeywords.contains(trimmed)) {
                    uniqueKeywords.add(trimmed);
                }
            }

            log.info("关键词优化完成: original={}, optimized={}", originalKeywords, uniqueKeywords);
            return uniqueKeywords;

        } catch (Exception e) {
            log.error("关键词优化失败，使用原始关键词: {}", originalKeywords, e);
            return List.of(originalKeywords);
        }
    }

    /**
     * 判断关键词是否已经是高质量的英文搜索词
     */
    private boolean isGoodEnglishKeywords(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return false;
        }
        // 简单判断：是否全为 ASCII 字符且包含空格（多个词）
        String trimmed = keywords.trim();
        boolean allAscii = trimmed.chars().allMatch(c -> c < 128);
        boolean hasMultipleWords = trimmed.contains(" ");
        return allAscii && hasMultipleWords && trimmed.length() >= 5;
    }

    /**
     * 关键词优化结果内部类
     */
    @Data
    public static class KeywordOptimizationResult {
        private List<String> optimizedKeywords;
        private String primaryKeyword;
    }
}
