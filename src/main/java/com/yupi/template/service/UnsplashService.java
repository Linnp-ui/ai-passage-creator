package com.yupi.template.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yupi.template.config.UnsplashConfig;
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
 * Unsplash 图片检索服务
 * 作为 Pexels 的补充/替代图库源，提供高质量摄影图片
 *
 * @author AI Passage Creator
 */
@Service
@Slf4j
public class UnsplashService implements ImageSearchService {

    @Resource
    private UnsplashConfig unsplashConfig;

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
        return ImageMethodEnum.UNSPLASH;
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
        String accessKey = unsplashConfig.getAccessKey();
        if (accessKey == null || accessKey.isBlank()) {
            log.warn("Unsplash Access Key 未配置");
            return candidates;
        }
        try {
            String url = buildSearchUrl(keywords, limit);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Client-ID " + accessKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Unsplash API 调用失败: {}", response.code());
                    return candidates;
                }

                String responseBody = response.body().string();
                return extractImageCandidates(responseBody, keywords);
            }
        } catch (IOException e) {
            log.error("Unsplash API 调用异常", e);
            return candidates;
        }
    }

    /**
     * 构建搜索 URL
     */
    private String buildSearchUrl(String keywords, int limit) {
        int perPage = Math.min(limit, unsplashConfig.getPerPage());
        return String.format("%s?query=%s&per_page=%d&orientation=%s",
                UNSPLASH_API_URL,
                java.net.URLEncoder.encode(keywords, java.nio.charset.StandardCharsets.UTF_8),
                perPage,
                unsplashConfig.getOrientation());
    }

    /**
     * 从响应中提取图片候选列表
     */
    private List<ImageCandidate> extractImageCandidates(String responseBody, String keywords) {
        List<ImageCandidate> candidates = new ArrayList<>();
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray results = jsonObject.getAsJsonArray("results");

        if (results == null || results.isEmpty()) {
            log.warn("Unsplash 未检索到图片: {}", keywords);
            return candidates;
        }

        for (int i = 0; i < results.size(); i++) {
            JsonObject photo = results.get(i).getAsJsonObject();
            JsonObject urls = photo.getAsJsonObject("urls");
            JsonObject user = photo.getAsJsonObject("user");

            String imageUrl = urls.has("regular") ? urls.get("regular").getAsString() : urls.get("small").getAsString();
            int width = photo.has("width") ? photo.get("width").getAsInt() : 0;
            int height = photo.has("height") ? photo.get("height").getAsInt() : 0;
            String photographer = user != null && user.has("name") ? user.get("name").getAsString() : "unknown";
            int likes = photo.has("likes") ? photo.get("likes").getAsInt() : 0;

            ImageCandidate candidate = ImageCandidate.builder()
                    .url(imageUrl)
                    .source("UNSPLASH")
                    .keyword(keywords)
                    .width(width)
                    .height(height)
                    .photographer(photographer)
                    .rawMetadata("{\"likes\":" + likes + "}")
                    .build();

            candidates.add(candidate);
        }

        log.info("Unsplash 检索成功: keywords={}, count={}", keywords, candidates.size());
        return candidates;
    }

    @Override
    public boolean isAvailable() {
        String accessKey = unsplashConfig.getAccessKey();
        return accessKey != null && !accessKey.isBlank();
    }
}
