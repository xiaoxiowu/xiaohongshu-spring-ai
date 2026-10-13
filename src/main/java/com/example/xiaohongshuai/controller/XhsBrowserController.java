package com.example.xiaohongshuai.controller;

import com.example.common.result.Result;
import com.example.xiaohongshuai.service.XhsBrowserTaskLock;
import com.example.xiaohongshuai.vo.XhsBrowserStatusVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/xhs/browser")
public class XhsBrowserController {

    private final XhsBrowserTaskLock xhsBrowserTaskLock;

    public XhsBrowserController(XhsBrowserTaskLock xhsBrowserTaskLock) {
        this.xhsBrowserTaskLock = xhsBrowserTaskLock;
    }

    @GetMapping("/status")
    public Result<XhsBrowserStatusVO> status() {
        return Result.success(XhsBrowserStatusVO.from(xhsBrowserTaskLock.status()));
    }
}
