package com.waiitz.ai_mini_service.client;

import com.waiitz.ai_mini_service.context.PromptVersion;
import com.waiitz.ai_mini_service.model.vo.AiChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
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
    @Resource
    private RedisTemplate<String , Object> redisTemplate;

    @Value("${openai.base-url}")
    private String API_URL ;
    @Value("${openai.api-key}")
    private String API_KEY ;
    @Value("${openai.model}")
    private String MODEL ;

    private final ObjectMapper objectMapper = new ObjectMapper();



    public AiChatResponse chat(String userMessage){
        try {
            String rawJson = callLLM(userMessage);
            AiChatResponse aiChatResponse = new AiChatResponse();
            aiChatResponse.setAnswer(rawJson);
            return aiChatResponse;
        }catch (Exception e){
            return fallbackResponse(e);
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
            throw new RuntimeException("model APi request failed" + response.getBody());
        }
        JsonNode root = objectMapper.readTree(response.getBody());

        return root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();


    }

    private AiChatResponse fallbackResponse(Exception e){
        return new AiChatResponse(
                "AI 服务暂时不可用 已进入 fallback 逻辑。错误信息" + e.getMessage(),
                List.of("检查API Key 是否正确","检查模型是否支持 structured outputs","查看服务段日志"),
                "unknown",
                false
        );
    }

    public void saveHisstory(String userId,String msg){
        Long l = redisTemplate.opsForList().rightPush(userId, msg);
        log.info("入库对话：{} {} {}",userId,msg,l);
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory instanceof LettuceConnectionFactory lcf) {
            log.info("========== Redis 连接信息 ==========");
            log.info("Host: {}", lcf.getHostName());
            log.info("Port: {}", lcf.getPort());
            log.info("Database: {}", lcf.getDatabase());
            log.info("Password: {}", lcf.getPassword() != null ? "***" : "无");
            log.info("=====================================");
        }
    }

    
}
