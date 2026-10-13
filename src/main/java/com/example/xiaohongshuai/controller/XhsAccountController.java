package com.example.xiaohongshuai.controller;

import com.example.common.result.Result;
import com.example.xiaohongshuai.service.XhsAccountService;
import com.example.xiaohongshuai.vo.AccountStatusVO;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/xhs/account")
public class XhsAccountController {

    private final XhsAccountService xhsAccountService;

    public XhsAccountController(XhsAccountService xhsAccountService) {
        this.xhsAccountService = xhsAccountService;
    }

    @GetMapping("/status")
    public Result<AccountStatusVO> status() {
        return Result.success(AccountStatusVO.from(xhsAccountService.status()));
    }

    @GetMapping("/qrcode")
    public Result<McpSchema.CallToolResult> qrcode() {
        return Result.success(xhsAccountService.qrcode());
    }

    @PostMapping("/reset")
    public Result<McpSchema.CallToolResult> reset() {
        return Result.success(xhsAccountService.reset());
    }
}
