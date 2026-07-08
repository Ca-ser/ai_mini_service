package com.waiitz.suji_service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;  // ✅ 改成RedisTemplate

    @Test
    public void testRedisConnection() {
        try {
            log.info("========== Redis测试开始 ==========");

            // 测试写入
            redisTemplate.opsForValue().set("test:ping", "pong");

            // 测试读取
            Object result = redisTemplate.opsForValue().get("test:ping");
            log.info("✅ Redis连接成功！测试值: {}", result);

            // 清理
            redisTemplate.delete("test:ping");

            log.info("========== Redis测试完成 ==========");

        } catch (Exception e) {
            log.error("❌ Redis连接失败", e);
        }
    }
}