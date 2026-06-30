package com.waiitz.ai_mini_service.controller;

import com.waiitz.ai_mini_service.client.LimClient;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    LimClient  limClient;

    @GetMapping("/chat")
    public List<String> chat(@RequestParam String message){
        ArrayList<String> strings = new ArrayList<>();
        strings.add(limClient.callLLM(message));
        strings.add(String.valueOf(limClient.estimateTokens(message)));
        return strings ;

    }
}
