package com.yupi.template.service;

import com.yupi.template.config.LocalImageStorageConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地图片存储服务
 * 替代 COS，将图片保存到本地服务器
 *
 * @author AI Passage Creator
 */
@Service
@Slf4j
public class LocalImageStorageService {

    @Resource
    private LocalImageStorageConfig storageConfig;

    private Path storagePath;

    @PostConstruct
    public void init() {
        // 解析存储路径
        String path = storageConfig.getPath();
        if (Paths.get(path).isAbsolute()) {
            storagePath = Paths.get(path);
        } else {
            // 相对路径，使用用户目录下的路径
            storagePath = Paths.get(System.getProperty("user.dir"), path);
        }

        // 创建存储目录
        try {
            Files.createDirectories(storagePath);
            log.info("本地图片存储路径: {}", storagePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("创建图片存储目录失败: {}", storagePath, e);
        }
    }

    /**
     * 保存图片字节数据到本地
     *
     * @param imageBytes 图片字节数据
     * @param mimeType   MIME 类型
     * @param folder     文件夹分类
     * @return 图片访问 URL
     */
    public String saveImage(byte[] imageBytes, String mimeType, String folder) {
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("图片字节数据为空，无法保存");
            return null;
        }

        try {
            // 生成文件名
            String extension = getExtensionFromMimeType(mimeType);
            String fileName = UUID.randomUUID() + extension;

            // 创建子目录
            Path folderPath = storagePath.resolve(folder);
            Files.createDirectories(folderPath);

            // 保存文件
            Path filePath = folderPath.resolve(fileName);
            Files.write(filePath, imageBytes);

            // 构建访问 URL
            String url = buildImageUrl(folder, fileName);
            log.info("图片保存成功, size={} bytes, path={}, url={}", imageBytes.length, filePath, url);

            return url;
        } catch (IOException e) {
            log.error("保存图片到本地失败", e);
            return null;
        }
    }

    /**
     * 构建图片访问 URL
     *
     * @param folder   文件夹
     * @param fileName 文件名
     * @return 完整 URL
     */
    private String buildImageUrl(String folder, String fileName) {
        String urlPrefix = storageConfig.getUrlPrefix();
        // 确保前缀以 / 开头
        if (!urlPrefix.startsWith("/")) {
            urlPrefix = "/" + urlPrefix;
        }

        // 获取服务器基础 URL
        String serverBaseUrl = storageConfig.getServerBaseUrl();
        if (serverBaseUrl == null || serverBaseUrl.isEmpty()) {
            // 默认使用本地开发环境地址
            serverBaseUrl = "http://localhost:8567";
        }
        // 确保 serverBaseUrl 不以 / 结尾
        if (serverBaseUrl.endsWith("/")) {
            serverBaseUrl = serverBaseUrl.substring(0, serverBaseUrl.length() - 1);
        }

        return serverBaseUrl + urlPrefix + "/" + folder + "/" + fileName;
    }

    /**
     * 根据 MIME 类型获取文件扩展名
     */
    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return ".png";
        }
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> ".png";
        };
    }

    /**
     * 获取存储路径
     */
    public Path getStoragePath() {
        return storagePath;
    }
}
