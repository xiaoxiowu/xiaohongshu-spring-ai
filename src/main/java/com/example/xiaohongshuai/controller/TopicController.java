package com.example.xiaohongshuai.controller;

import java.util.List;

import com.example.common.result.Result;
import com.example.xiaohongshuai.dto.CreateTopicDTO;
import com.example.xiaohongshuai.service.TopicService;
import com.example.xiaohongshuai.vo.TopicVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping
    public Result<TopicVO> create(@Valid @RequestBody CreateTopicDTO request) {
        return Result.success(TopicVO.from(topicService.create(request.title(), request.keywords(),
                request.audience(), request.direction())));
    }

    @GetMapping
    public Result<List<TopicVO>> list() {
        return Result.success(topicService.list().stream().map(TopicVO::from).toList());
    }
}
