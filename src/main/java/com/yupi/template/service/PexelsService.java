package com.yupi.template.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yupi.template.config.PexelsConfig;
import com.yupi.template.model.dto.image.ImageCandidate;
import com.yupi.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.yupi.template.constant.ArticleConstant.*;

/**
 * Pexels 图片检索服务（增强版）
 * 支持返回多张图片候选及质量元数据
 *
 * @author <a href="https://codefather.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class PexelsService implements ImageSearchService {

    @Resource
    private PexelsConfig pexelsConfig;

    // 使用带超时的 OkHttpClient
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(15))
            .writeTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String searchImage(String keywords) {
        List<ImageCandidate> candidates = searchImages(keywords, 1);
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(0).getUrl();
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.PEXELS;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }

    /**
     * 搜索多张图片并返回候选列表（带质量元数据）
     *
     * @param keywords 搜索关键词
     * @param limit    最大返回数量
     * @return 图片候选列表
     */
    public List<ImageCandidate> searchImages(String keywords, int limit) {
        List<ImageCandidate> candidates = new ArrayList<>();
        if (keywords == null || keywords.trim().isEmpty() || limit <= 0) {
            return candidates;
        }
        String apiKey = pexelsConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Pexels API Key 未配置");
            return candidates;
        }
        try {
            String url = buildSearchUrl(keywords, limit);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Pexels API 调用失败: {}", response.code());
                    return candidates;
                }

                String responseBody = response.body().string();
                return extractImageCandidates(responseBody, keywords);
            }
        } catch (IOException e) {
            log.error("Pexels API 调用异常", e);
            return candidates;
        }
    }

    /**
     * 构建搜索 URL
     */
    private String buildSearchUrl(String keywords, int limit) {
        int perPage = Math.max(limit, PEXELS_PER_PAGE);
        return String.format("%s?query=%s&per_page=%d&orientation=%s",
                PEXELS_API_URL,
                java.net.URLEncoder.encode(keywords, java.nio.charset.StandardCharsets.UTF_8),
                perPage,
                PEXELS_ORIENTATION_LANDSCAPE);
    }

    /**
     * 从响应中提取图片候选列表
     */
    private List<ImageCandidate> extractImageCandidates(String responseBody, String keywords) {
        List<ImageCandidate> candidates = new ArrayList<>();
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray photos = jsonObject.getAsJsonArray("photos");

        if (photos == null || photos.isEmpty()) {
            log.warn("Pexels 未检索到图片: {}", keywords);
            return candidates;
        }

        for (int i = 0; i < photos.size(); i++) {
            JsonObject photo = photos.get(i).getAsJsonObject();
            JsonObject src = photo.getAsJsonObject("src");

            String imageUrl = src.has("large") ? src.get("large").getAsString() : src.get("medium").getAsString();
            int width = photo.has("width") ? photo.get("width").getAsInt() : 0;
            int height = photo.has("height") ? photo.get("height").getAsInt() : 0;
            String photographer = photo.has("photographer") ? photo.get("photographer").getAsString() : "unknown";

            ImageCandidate candidate = ImageCandidate.builder()
                    .url(imageUrl)
                    .source("PEXELS")
                    .keyword(keywords)
                    .width(width)
                    .height(height)
                    .photographer(photographer)
                    .rawMetadata(photo.toString())
                    .build();

            candidates.add(candidate);
        }

        log.info("Pexels 检索成功: keywords={}, count={}", keywords, candidates.size());
        return candidates;
    }

    @Override
    public boolean isAvailable() {
        String apiKey = pexelsConfig.getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }
}
