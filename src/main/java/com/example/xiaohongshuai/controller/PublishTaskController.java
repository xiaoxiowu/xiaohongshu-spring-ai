package com.example.xiaohongshuai.controller;

import java.util.List;

import com.example.common.result.Result;
import com.example.xiaohongshuai.service.PublishTaskService;
import com.example.xiaohongshuai.vo.PublishTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publish-tasks")
public class PublishTaskController {

    private final PublishTaskService publishTaskService;

    public PublishTaskController(PublishTaskService publishTaskService) {
        this.publishTaskService = publishTaskService;
    }

    @GetMapping
    public Result<List<PublishTaskVO>> list() {
        return Result.success(publishTaskService.list().stream().map(PublishTaskVO::from).toList());
    }

    @GetMapping("/{id}")
    public Result<PublishTaskVO> detail(@PathVariable Long id) {
        return Result.success(PublishTaskVO.from(publishTaskService.getRequired(id)));
    }
}
