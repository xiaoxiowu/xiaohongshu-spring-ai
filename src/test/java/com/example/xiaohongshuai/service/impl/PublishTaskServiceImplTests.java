package com.example.xiaohongshuai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublishTaskServiceImplTests {

    private final PublishTaskServiceImpl service = new PublishTaskServiceImpl(null, null, null, null,
            new ObjectMapper());

    @Test
    void parsesPublishedNoteIdAndUrlFromNestedText() {
        PublishTaskServiceImpl.PublishResult result = service.parsePublishResult("""
                {"content":[{"text":"{\\"noteUrl\\":\\"https://www.xiaohongshu.com/explore/abc123\\",\\"noteId\\":\\"abc123\\"}"}]}
                """);

        assertThat(result.noteId()).isEqualTo("abc123");
        assertThat(result.noteUrl()).isEqualTo("https://www.xiaohongshu.com/explore/abc123");
    }

    @Test
    void parsesErrorMessageFromResponse() {
        PublishTaskServiceImpl.PublishResult result = service.parsePublishResult("""
                {"content":[{"text":"{\\"errorMessage\\":\\"图片上传失败\\"}"}]}
                """);

        assertThat(result.errorMessage()).isEqualTo("图片上传失败");
    }
}
