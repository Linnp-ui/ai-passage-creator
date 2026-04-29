package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Hugging Face AI 生图配置
 *
 * @author AI Passage Creator
 */
@Configuration
@ConfigurationProperties(prefix = "huggingface")
@Data
public class HuggingFaceConfig {

    /**
     * Hugging Face API Token
     */
    private String apiToken = "";

    /**
     * 使用的模型
     */
    private String model = "black-forest-labs/FLUX.1-schnell";

    /**
     * 图片宽度
     */
    private int width = 1024;

    /**
     * 图片高度
     */
    private int height = 1024;

    /**
     * 代理主机（可选，用于访问 Hugging Face）
     */
    private String proxyHost;

    /**
     * 代理端口（可选）
     */
    private int proxyPort = 7890;

    /**
     * Inference Provider（可选，默认使用 hf-inference）
     * 可选值: hf-inference, fal-ai, replicate, together, novita 等
     */
    private String provider = "hf-inference";
}
