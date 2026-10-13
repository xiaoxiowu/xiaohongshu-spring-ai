package com.example.xiaohongshuai.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatDTO(@NotBlank String message) {
}
