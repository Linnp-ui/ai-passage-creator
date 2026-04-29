package com.yupi.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.template.config.DashScopeImageConfig;
import com.yupi.template.model.dto.image.ImageData;
import com.yupi.template.model.dto.image.ImageRequest;
import com.yupi.template.model.enums.ImageMethodEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * DashScope 通义万相图片生成服务
 *
 * @author AI Passage Creator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashScopeImageService implements ImageSearchService {

    private final DashScopeImageConfig dashScopeImageConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestTemplate restTemplate;

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.DASHSCOPE;
    }

    @Override
    public String searchImage(String prompt) {
        // 先生成图片，然后下载字节数据
        ImageData imageData = generateAndDownloadImage(prompt);
        if (imageData != null && imageData.getBytes() != null) {
            // 返回临时 URL，实际会通过 getImageData 保存到本地
            return "dashscope://" + System.currentTimeMillis();
        }
        return null;
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        String param = request.getEffectiveParam(getMethod().isAiGenerated());
        return generateAndDownloadImage(param);
    }

    /**
     * 生成图片并下载字节数据
     */
    private ImageData generateAndDownloadImage(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            log.warn("DashScope 生成图片失败: prompt 为空");
            return null;
        }

        try {
            String apiUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + dashScopeImageConfig.getApiKey());
            headers.set("X-DashScope-Async", "enable");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", dashScopeImageConfig.getModel());

            Map<String, Object> input = new HashMap<>();
            input.put("prompt", prompt);
            requestBody.put("input", input);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("size", dashScopeImageConfig.getSize() + "*" + dashScopeImageConfig.getSize());
            parameters.put("n", dashScopeImageConfig.getN());
            parameters.put("style", dashScopeImageConfig.getStyle());
            requestBody.put("parameters", parameters);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("DashScope 开始生成图片, model={}, prompt长度={}", dashScopeImageConfig.getModel(), prompt.length());

            ResponseEntity<String> response = getRestTemplate().postForEntity(apiUrl, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("DashScope API 调用失败: {}", response.getStatusCode());
                return null;
            }

            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String taskId = responseJson.path("output").path("task_id").asText();

            if (taskId == null || taskId.isEmpty()) {
                String errorMsg = responseJson.path("message").asText("未知错误");
                log.error("DashScope 创建任务失败: {}", errorMsg);
                return null;
            }

            log.info("DashScope 任务已创建, taskId={}", taskId);

            String imageUrl = pollTaskResult(taskId);

            if (imageUrl != null) {
                log.info("DashScope 图片生成成功, url={}, 开始下载...", imageUrl);
                // 下载图片字节数据
                return downloadImage(imageUrl);
            }

            return null;

        } catch (Exception e) {
            log.error("DashScope 图片生成异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 下载图片
     * 注意：DashScope 返回的 OSS URL 已经带有临时签名，直接发送简单 GET 请求即可，不要添加任何 header
     */
    private ImageData downloadImage(String imageUrl) {
        try {
            // 使用 Java 11+ HttpClient 下载图片
            // 这种方式可以正确处理预签名 URL，避免 URL 编码问题
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                byte[] imageBytes = response.body();
                // 根据 URL 推断 MIME 类型
                String mimeType = "image/png";
                if (imageUrl.endsWith(".jpg") || imageUrl.endsWith(".jpeg")) {
                    mimeType = "image/jpeg";
                } else if (imageUrl.endsWith(".webp")) {
                    mimeType = "image/webp";
                }
                log.info("DashScope 图片下载成功, size={} bytes", imageBytes.length);
                return ImageData.fromBytes(imageBytes, mimeType);
            } else {
                log.error("DashScope 图片下载失败, HTTP状态码: {}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("DashScope 图片下载失败: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getFallbackImage(int position) {
        return "https://picsum.photos/800/600?random=" + position;
    }

    private String pollTaskResult(String taskId) {
        String statusUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId;
        int maxAttempts = 60;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                Thread.sleep(2000);
                attempt++;

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + dashScopeImageConfig.getApiKey());

                HttpEntity<Void> request = new HttpEntity<>(headers);
                ResponseEntity<String> response = getRestTemplate().exchange(
                        statusUrl, org.springframework.http.HttpMethod.GET, request, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode responseJson = objectMapper.readTree(response.getBody());
                    String status = responseJson.path("output").path("task_status").asText();

                    if ("SUCCEEDED".equals(status)) {
                        JsonNode results = responseJson.path("output").path("results");
                        if (results.isArray() && results.size() > 0) {
                            return results.get(0).path("url").asText();
                        }
                    } else if ("FAILED".equals(status)) {
                        String errorMsg = responseJson.path("output").path("message").asText("任务失败");
                        log.error("DashScope 任务失败: {}", errorMsg);
                        return null;
                    }

                    log.debug("DashScope 任务状态: {}, 尝试 {}/{}", status, attempt, maxAttempts);
                }
            } catch (Exception e) {
                log.warn("DashScope 查询任务状态异常: {}", e.getMessage());
            }
        }

        log.error("DashScope 任务超时, taskId={}", taskId);
        return null;
    }

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }
        return restTemplate;
    }
}
