package com.example.xiaohongshuai.controller;

import com.example.common.result.Result;
import com.example.xiaohongshuai.dto.TopicAnalysisSearchDTO;
import com.example.xiaohongshuai.service.TopicAnalysisJobService;
import com.example.xiaohongshuai.service.TopicAnalysisService;
import com.example.xiaohongshuai.service.XhsBrowserTaskLock;
import com.example.xiaohongshuai.vo.TopicAnalysisJobVO;
import com.example.xiaohongshuai.vo.TopicAnalysisVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topic-analysis")
public class TopicAnalysisController {

    private final TopicAnalysisService topicAnalysisService;
    private final TopicAnalysisJobService topicAnalysisJobService;
    private final XhsBrowserTaskLock xhsBrowserTaskLock;

    public TopicAnalysisController(TopicAnalysisService topicAnalysisService,
            TopicAnalysisJobService topicAnalysisJobService, XhsBrowserTaskLock xhsBrowserTaskLock) {
        this.topicAnalysisService = topicAnalysisService;
        this.topicAnalysisJobService = topicAnalysisJobService;
        this.xhsBrowserTaskLock = xhsBrowserTaskLock;
    }

    @PostMapping("/search")
    public Result<TopicAnalysisVO> search(@Valid @RequestBody TopicAnalysisSearchDTO request) {
        return Result.success(xhsBrowserTaskLock.runExclusive("热门选题采集", () ->
                topicAnalysisService.search(request.keyword(), request.limit(),
                        request.sortBy(), request.noteType(), request.publishTime())));
    }

    @PostMapping("/jobs")
    public Result<TopicAnalysisJobVO> startJob(@Valid @RequestBody TopicAnalysisSearchDTO request) {
        return Result.success(topicAnalysisJobService.start(request));
    }

    @GetMapping("/jobs/{id}")
    public Result<TopicAnalysisJobVO> job(@PathVariable String id) {
        return Result.success(topicAnalysisJobService.getRequired(id));
    }
}
