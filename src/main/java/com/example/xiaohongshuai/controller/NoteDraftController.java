package com.example.xiaohongshuai.controller;

import java.util.List;

import com.example.common.result.Result;
import com.example.xiaohongshuai.dto.GenerateNoteDTO;
import com.example.xiaohongshuai.dto.PublishNoteDTO;
import com.example.xiaohongshuai.dto.UpdateNoteDTO;
import com.example.xiaohongshuai.service.NoteDraftService;
import com.example.xiaohongshuai.service.PublishTaskService;
import com.example.xiaohongshuai.vo.NoteDraftVO;
import com.example.xiaohongshuai.vo.PublishTaskVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NoteDraftController {

    private final NoteDraftService noteDraftService;
    private final PublishTaskService publishTaskService;

    public NoteDraftController(NoteDraftService noteDraftService, PublishTaskService publishTaskService) {
        this.noteDraftService = noteDraftService;
        this.publishTaskService = publishTaskService;
    }

    @PostMapping("/generate")
    public Result<NoteDraftVO> generate(@Valid @RequestBody GenerateNoteDTO request) {
        return Result.success(NoteDraftVO.from(noteDraftService.generate(request.topicId(), request.title(),
                request.keywords(), request.audience(), request.direction())));
    }

    @GetMapping
    public Result<List<NoteDraftVO>> list() {
        return Result.success(noteDraftService.list().stream().map(NoteDraftVO::from).toList());
    }

    @GetMapping("/{id}")
    public Result<NoteDraftVO> detail(@PathVariable Long id) {
        return Result.success(NoteDraftVO.from(noteDraftService.getRequired(id)));
    }

    @PutMapping("/{id}")
    public Result<NoteDraftVO> update(@PathVariable Long id, @RequestBody UpdateNoteDTO request) {
        return Result.success(NoteDraftVO.from(noteDraftService.update(id, request.title(), request.content(),
                request.tags(), request.imagePrompt(), request.imageAssetIds(), request.status())));
    }

    @PostMapping("/{id}/publish")
    public Result<PublishTaskVO> publish(@PathVariable Long id, @RequestBody PublishNoteDTO request) {
        PublishTaskService.PublishRequest serviceRequest = new PublishTaskService.PublishRequest(
                request.type(), request.images(), request.imageAssetIds(), request.video(),
                request.scheduleAt(), request.visibility(), request.isOriginal());
        return Result.success(PublishTaskVO.from(publishTaskService.publish(id, serviceRequest)));
    }
}
