package com.example.xiaohongshuai.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTopicDTO(
        @NotBlank String title,
        String keywords,
        String audience,
        String direction) {
}
