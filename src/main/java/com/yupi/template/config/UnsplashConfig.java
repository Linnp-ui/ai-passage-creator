package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Unsplash 图库配置
 *
 * @author AI Passage Creator
 */
@Configuration
@ConfigurationProperties(prefix = "unsplash")
@Data
public class UnsplashConfig {

    /**
     * Access Key
     */
    private String accessKey;

    /**
     * 每页返回数量
     */
    private int perPage = 10;

    /**
     * 图片方向：landscape（横向）, portrait（纵向）, squarish（方形）
     */
    private String orientation = "landscape";
}
