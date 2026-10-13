package com.example.xiaohongshuai.controller;

import java.util.List;

import com.example.common.result.Result;
import com.example.xiaohongshuai.dto.GenerateImageDTO;
import com.example.xiaohongshuai.dto.ImportImageUrlDTO;
import com.example.xiaohongshuai.dto.UpdateImageAssetDTO;
import com.example.xiaohongshuai.service.ImageAssetService;
import com.example.xiaohongshuai.vo.ImageAssetVO;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/images")
public class ImageAssetController {

    private final ImageAssetService imageAssetService;

    public ImageAssetController(ImageAssetService imageAssetService) {
        this.imageAssetService = imageAssetService;
    }

    @PostMapping("/generate")
    public Result<ImageAssetVO> generate(@Valid @RequestBody GenerateImageDTO request) {
        ImageAssetService.GenerateImageRequest serviceRequest = new ImageAssetService.GenerateImageRequest(
                request.name(), request.prompt(), request.tags(), request.usageScene(),
                request.aspectRatio(), request.remark());
        return Result.success(ImageAssetVO.from(imageAssetService.generate(serviceRequest)));
    }

    @PostMapping("/import-url")
    public Result<ImageAssetVO> importUrl(@Valid @RequestBody ImportImageUrlDTO request) {
        ImageAssetService.ImportImageUrlRequest serviceRequest = new ImageAssetService.ImportImageUrlRequest(
                request.name(), request.url(), request.prompt(), request.tags(), request.usageScene(),
                request.aspectRatio(), request.remark());
        return Result.success(ImageAssetVO.from(imageAssetService.importUrl(serviceRequest)));
    }

    @GetMapping
    public Result<List<ImageAssetVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status) {
        return Result.success(imageAssetService.list(keyword, tag, source, status).stream()
                .map(ImageAssetVO::from)
                .toList());
    }

    @GetMapping("/{id}")
    public Result<ImageAssetVO> detail(@PathVariable Long id) {
        return Result.success(ImageAssetVO.from(imageAssetService.getRequired(id)));
    }

    @PutMapping("/{id}")
    public Result<ImageAssetVO> update(@PathVariable Long id, @RequestBody UpdateImageAssetDTO request) {
        ImageAssetService.UpdateImageAssetRequest serviceRequest = new ImageAssetService.UpdateImageAssetRequest(
                request.name(), request.tags(), request.usageScene(), request.aspectRatio(),
                request.status(), request.remark());
        return Result.success(ImageAssetVO.from(imageAssetService.update(id, serviceRequest)));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(imageAssetService.file(id));
    }
}
