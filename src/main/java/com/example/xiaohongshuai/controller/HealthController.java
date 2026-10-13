package com.example.xiaohongshuai.controller;

import java.time.OffsetDateTime;

import com.example.common.result.Result;
import com.example.xiaohongshuai.vo.HealthVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<HealthVO> health() {
        return Result.success(new HealthVO("ok", "在线", "xiaohongshu-spring-ai", OffsetDateTime.now().toString()));
    }
}

