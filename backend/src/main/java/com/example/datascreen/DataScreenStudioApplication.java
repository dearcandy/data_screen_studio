package com.example.datascreen;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.datascreen.repository")
public class DataScreenStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataScreenStudioApplication.class, args);
    }
}
