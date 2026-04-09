package com.example.datascreen.service;

import com.example.datascreen.config.StudioProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Studio 文件路径管理，负责上传根目录初始化与安全路径解析。
 */
@Component
@RequiredArgsConstructor
public class StudioPaths {

    /**
     * 文件存储位置
     */
    private final StudioProperties properties;
    @Getter
    private Path uploadRoot;


    /** 启动时创建上传目录
     * 初始化上传根目录，确保存在且可写。
     */
    @PostConstruct
    void init() throws Exception {
        uploadRoot = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
    }

    /**
     * 解析上传文件路径并防止目录穿越攻击。
     * @param fileName 文件名
     * @return 完全路径
     */
    public Path resolveUpload(String fileName) {
        Path p = uploadRoot.resolve(fileName).normalize();
        if (!p.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("非法路径");
        }
        return p;
    }
}
