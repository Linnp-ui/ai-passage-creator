package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 本地图片存储配置
 *
 * @author AI Passage Creator
 */
@Configuration
@ConfigurationProperties(prefix = "local.image-storage")
@Data
public class LocalImageStorageConfig {

    /**
     * 图片存储路径（相对于项目根目录或绝对路径）
     */
    private String path = "uploads/images";

    /**
     * 图片访问 URL 前缀
     */
    private String urlPrefix = "/api/images";

    /**
     * 服务器基础 URL（用于构建完整图片 URL）
     */
    private String serverBaseUrl = "";

    /**
     * 是否启用本地存储（替代 COS）
     */
    private boolean enabled = true;
}
