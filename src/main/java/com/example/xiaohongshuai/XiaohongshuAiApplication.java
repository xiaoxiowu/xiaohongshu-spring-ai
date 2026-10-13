package com.example.xiaohongshuai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.xiaohongshuai.mapper")
@SpringBootApplication
public class XiaohongshuAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohongshuAiApplication.class, args);
    }
}

