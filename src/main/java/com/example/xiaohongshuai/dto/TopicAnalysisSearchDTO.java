package com.example.xiaohongshuai.dto;

import jakarta.validation.constraints.NotBlank;

public record TopicAnalysisSearchDTO(
        @NotBlank String keyword,
        Integer limit,
        String sortBy,
        String noteType,
        String publishTime) {
}
