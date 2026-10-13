package com.example.xiaohongshuai.dto;

import java.util.List;

public record PublishNoteDTO(
        String type,
        List<String> images,
        List<Long> imageAssetIds,
        String video,
        String scheduleAt,
        String visibility,
        Boolean isOriginal) {
}
