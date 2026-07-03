package com.waiitz.ai_mini_service.controller;

import com.waiitz.ai_mini_service.client.LimClient;
import com.waiitz.ai_mini_service.model.AiChatResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
