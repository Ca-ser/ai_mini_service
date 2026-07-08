package com.waiitz.suji_service.controller;

import com.waiitz.suji_service.client.LimClient;
import com.waiitz.suji_service.model.vo.AiChatResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiController {
    @Resource
    private LimClient limClient;


    @GetMapping("/chat")
    public AiChatResponse chat(
            @RequestParam String userId,
            @RequestParam String message){
        return limClient.chat(message);




    }
}
