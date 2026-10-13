package com.example.xiaohongshuai.vo;

import com.example.xiaohongshuai.common.StatusText;
import com.example.xiaohongshuai.service.XhsAccountService;

public record AccountStatusVO(
        boolean mcpOnline,
        String mcpStatusText,
        String loginStatus,
        String loginStatusText,
        String nickname,
        String avatarUrl,
        String userId,
        String rawResponse,
        String errorMessage) {

    public static AccountStatusVO from(XhsAccountService.AccountStatus status) {
        return new AccountStatusVO(status.mcpOnline(), status.mcpOnline() ? "可用" : "不可用",
                status.loginStatus(), StatusText.status(status.loginStatus()),
                status.nickname(), status.avatarUrl(), status.userId(),
                status.rawResponse(), status.errorMessage());
    }
}
