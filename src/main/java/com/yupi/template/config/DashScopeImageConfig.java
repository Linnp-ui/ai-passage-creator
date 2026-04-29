package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DashScope 通义万相图片生成配置
 *
 * @author AI Passage Creator
 */
@Configuration
@ConfigurationProperties(prefix = "dashscope.image")
@Data
public class DashScopeImageConfig {

    private String apiKey;

    private String model = "wanx-v1";

    private int size = 1024;

    private int n = 1;

    private String style = "<auto>";
}
