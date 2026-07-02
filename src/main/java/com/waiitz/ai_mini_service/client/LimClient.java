package com.waiitz.ai_mini_service.client;

import com.waiitz.ai_mini_service.context.PromptVersion;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class LimClient {
    @Resource
    private RedisTemplate<String , Object> redisTemplate;
    @Resource
    PromptVersion promptVersion;

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String API_KEY = "";

    
    public String callLLM(String userInput){
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        HashMap<String, Object> body = new HashMap<>();
        body.put("model","deepseek-v4-flash");
        ArrayList<Map<String , String >> messages = new ArrayList<>();
        messages.add(Map.of("role","system","content",promptVersion.getV1()));
        messages.add(Map.of("role","user","content",userInput));
        body.put("messages",messages);

        HttpEntity<HashMap<String, Object>> request = new HttpEntity<>(body, headers);
        String response = restTemplate.postForObject(API_URL, request, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        String content = root.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
        String user = "admin";
        log.info(content);
        saveHisstory(user,content);
        return response;


    }
    public int estimateTokens(String userInput){
        return userInput.length() / 2;
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
