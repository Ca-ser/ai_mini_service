package com.waiitz.suji_service.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.waiitz.suji_service.client.LimClient;
import com.waiitz.suji_service.common.BizException;
import com.waiitz.suji_service.common.RateLimiter;
import com.waiitz.suji_service.model.dto.AiChatRequest;
import com.waiitz.suji_service.model.vo.AiChatResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    @Resource
    private LimClient limClient;

    @Resource
    private RateLimiter rateLimiter;

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        String userId = (String) StpUtil.getLoginId();
        if (!rateLimiter.tryAcquire("ai:chat:" + userId)) {
            throw BizException.rateLimited("请求过于频繁，每分钟最多10次");
        }
        return limClient.chat(request.getMessage());
    }
}
