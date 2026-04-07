package com.example.datascreen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储位置
 */
@ConfigurationProperties(prefix = "studio")
public class StudioProperties {

    private String uploadDir = "./data/uploads";

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}
