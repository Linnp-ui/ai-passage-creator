package com.yupi.template.controller;

import com.yupi.template.service.LocalImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 图片访问控制器
 * 提供本地存储图片的访问接口
 *
 * @author AI Passage Creator
 */
@RestController
@RequestMapping("/images")
@Slf4j
public class ImageController {

    @Resource
    private LocalImageStorageService localImageStorageService;

    /**
     * 访问本地存储的图片
     *
     * @param folder   文件夹
     * @param filename 文件名
     * @return 图片数据
     */
    @GetMapping("/{folder}/{filename:.+}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable String folder,
            @PathVariable String filename,
            HttpServletRequest request) {

        try {
            Path imagePath = localImageStorageService.getStoragePath()
                    .resolve(folder)
                    .resolve(filename);

            // 安全检查：确保路径在存储目录内
            if (!imagePath.normalize().startsWith(localImageStorageService.getStoragePath().normalize())) {
                log.warn("非法的图片访问路径: {}", request.getRequestURI());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!Files.exists(imagePath)) {
                log.warn("图片不存在: {}", imagePath);
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            String contentType = determineContentType(filename);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setCacheControl("public, max-age=86400"); // 缓存1天

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("读取图片失败: folder={}, filename={}", folder, filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件名确定 Content-Type
     */
    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
