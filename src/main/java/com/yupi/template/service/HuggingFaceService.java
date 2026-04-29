package com.yupi.template.service;

import com.yupi.template.config.HuggingFaceConfig;
import com.yupi.template.model.dto.image.ImageData;
import com.yupi.template.model.dto.image.ImageRequest;
import com.yupi.template.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.yupi.template.constant.ArticleConstant.PICSUM_URL_TEMPLATE;

/**
 * Hugging Face AI 生图服务
 * 使用 Hugging Face Inference API 生成图片
 *
 * @author AI Passage Creator
 */
@Service
@Slf4j
public class HuggingFaceService implements ImageSearchService {

    @Resource
    private HuggingFaceConfig huggingFaceConfig;

    private RestTemplate restTemplate;

    /**
     * 初始化 RestTemplate  with 超时配置
     */
    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            // 连接超时：30秒
            factory.setConnectTimeout(30000);
            // 读取超时：120秒（生图需要较长时间）
            factory.setReadTimeout(120000);
            
            // 配置代理（如果设置了代理主机）
            String proxyHost = huggingFaceConfig.getProxyHost();
            if (proxyHost != null && !proxyHost.isEmpty()) {
                int proxyPort = huggingFaceConfig.getProxyPort();
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                factory.setProxy(proxy);
                log.info("Hugging Face 使用代理: {}:{}", proxyHost, proxyPort);
            }
            
            restTemplate = new RestTemplate(factory);
        }
        return restTemplate;
    }

    @Override
    public String searchImage(String keywords) {
        // 此方法已废弃，请使用 getImageData()
        return null;
    }

    @Override
    public ImageData getImageData(ImageRequest request) {
        String prompt = request.getEffectiveParam(true);
        return generateImageData(prompt);
    }

    /**
     * 根据提示词生成图片数据
     *
     * @param prompt 生图提示词
     * @return ImageData 包含图片字节数据，生成失败返回 null
     */
    public ImageData generateImageData(String prompt) {
        String apiToken = huggingFaceConfig.getApiToken();
        if (apiToken == null || apiToken.isEmpty()) {
            log.warn("Hugging Face API Token 未配置");
            return null;
        }

        try {
            String model = huggingFaceConfig.getModel();
            String provider = huggingFaceConfig.getProvider();
            if (provider == null || provider.isEmpty()) {
                provider = "hf-inference";
            }
            String apiUrl = "https://router.huggingface.co/" + provider + "/models/" + model;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "image/png");

            // 构建请求体 - 简化为只发送 inputs
            Map<String, Object> requestBody = Map.of(
                    "inputs", prompt
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Hugging Face 开始生成图片, model={}, prompt长度={}", model, prompt.length());
            long startTime = System.currentTimeMillis();

            // 调用 Hugging Face API
            ResponseEntity<byte[]> response = getRestTemplate().exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            long duration = System.currentTimeMillis() - startTime;
            byte[] imageBytes = response.getBody();

            // 检查响应是否有效（至少要有几个字节）
            if (imageBytes != null && imageBytes.length > 100) {
                log.info("Hugging Face 图片生成成功, size={} bytes, 耗时={}ms", imageBytes.length, duration);
                return ImageData.fromBytes(imageBytes, "image/png");
            }

            // 如果返回数据太小，可能是错误信息
            if (imageBytes != null) {
                String errorMsg = new String(imageBytes, StandardCharsets.UTF_8);
                log.warn("Hugging Face 返回数据异常, 可能是错误: {}", errorMsg);
            } else {
                log.warn("Hugging Face 返回空数据");
            }

            log.warn("Hugging Face 未生成图片, prompt={}", prompt);
            return null;

        } catch (RestClientException e) {
            log.error("Hugging Face API 调用失败, prompt长度={}, error={}", prompt.length(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Hugging Face 生成图片异常, prompt长度={}", prompt.length(), e);
            return null;
        }
    }

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.HUGGINGFACE;
    }

    @Override
    public String getFallbackImage(int position) {
        return String.format(PICSUM_URL_TEMPLATE, position);
    }

    @Override
    public boolean isAvailable() {
        String apiToken = huggingFaceConfig.getApiToken();
        return apiToken != null && !apiToken.isEmpty();
    }
}
