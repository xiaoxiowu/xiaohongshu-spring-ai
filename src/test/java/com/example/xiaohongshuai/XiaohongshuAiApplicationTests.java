package com.example.xiaohongshuai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.ai.mcp.client.enabled=false",
        "spring.ai.openai.api-key=test"
})
class XiaohongshuAiApplicationTests {

    @Test
    void contextLoads() {
    }
}

