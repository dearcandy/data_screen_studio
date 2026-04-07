package com.example.datascreen.service;

import com.example.datascreen.config.StudioProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Studio 文件路径管理，负责上传根目录初始化与安全路径解析。
 */
@Component
public class StudioPaths {

    /**
     * 文件存储位置
     */
    private final StudioProperties properties;
    @Getter
    private Path uploadRoot;

    public StudioPaths(StudioProperties properties) {
        this.properties = properties;
    }

    /** 启动时创建上传目录。 */
    @PostConstruct
    void init() throws Exception {
        uploadRoot = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
    }

    /**
     * 解析上传文件路径并防止目录穿越。
     */
    public Path resolveUpload(String fileName) {
        Path p = uploadRoot.resolve(fileName).normalize();
        if (!p.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("非法路径");
        }
        return p;
    }
}
