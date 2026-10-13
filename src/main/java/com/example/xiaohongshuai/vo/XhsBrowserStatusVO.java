package com.example.xiaohongshuai.vo;

import java.time.LocalDateTime;

import com.example.xiaohongshuai.service.XhsBrowserTaskLock;

public record XhsBrowserStatusVO(
        boolean busy,
        String taskName,
        LocalDateTime startedAt) {

    public static XhsBrowserStatusVO from(XhsBrowserTaskLock.Status status) {
        return new XhsBrowserStatusVO(status.busy(), status.taskName(), status.startedAt());
    }
}
