package com.waiitz.suji_service.client;

import com.waiitz.suji_service.context.PromptVersion;
import com.waiitz.suji_service.model.vo.AiChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@Service
public class LimClient {

    @Value("${llm.base-url}")
    private String API_URL;
    @Value("${llm.api-key}")
    private String API_KEY;
    @Value("${llm.model}")
    private String MODEL;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiChatResponse chat(String userMessage) {
        try {
            String rawJson = callLLM(userMessage);
            AiChatResponse aiChatResponse = new AiChatResponse();
            aiChatResponse.setAnswer(rawJson);
            return aiChatResponse;
        } catch (Exception e) {
            log.error("AI chat failed", e);
            return fallbackResponse();
        }
    }

    public String callLLM(String userInput) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        HashMap<String, Object> body = new HashMap<>();
        body.put("model", MODEL);

        ArrayList<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", PromptVersion.V1));
        messages.add(Map.of("role", "user", "content", userInput));

        body.put("messages", messages);
        body.put("temperature", 0.3);

        HttpEntity<HashMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("model API request failed: " + response.getStatusCode());
        }
        JsonNode root = objectMapper.readTree(response.getBody());

        return root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
    }

    private AiChatResponse fallbackResponse() {
        return new AiChatResponse(
                "AI 服务暂时不可用，请稍后重试。",
                List.of("请稍后重试"),
                "unknown",
                false
        );
    }
}
