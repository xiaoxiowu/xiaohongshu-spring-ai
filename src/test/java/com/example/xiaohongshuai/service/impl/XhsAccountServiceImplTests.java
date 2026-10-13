package com.example.xiaohongshuai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XhsAccountServiceImplTests {

    private final XhsAccountServiceImpl service = new XhsAccountServiceImpl(null, null, new ObjectMapper());

    @Test
    void parsesLoggedInAccountProfile() {
        XhsAccountServiceImpl.ParsedAccount account = service.parseAccountStatus("""
                {"content":[{"text":"{\\"isLoggedIn\\":true,\\"nickname\\":\\"运营号\\",\\"avatarUrl\\":\\"https://example.com/a.png\\",\\"userId\\":\\"u1\\"}"}]}
                """, false);

        assertThat(account.loginStatus()).isEqualTo("LOGGED_IN");
        assertThat(account.nickname()).isEqualTo("运营号");
        assertThat(account.avatarUrl()).isEqualTo("https://example.com/a.png");
        assertThat(account.userId()).isEqualTo("u1");
    }

    @Test
    void parsesNotLoggedInText() {
        XhsAccountServiceImpl.ParsedAccount account = service.parseAccountStatus("""
                {"content":[{"text":"当前未登录，请扫码登录"}]}
                """, false);

        assertThat(account.loginStatus()).isEqualTo("NOT_LOGGED_IN");
    }
}
