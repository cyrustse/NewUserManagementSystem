package com.ums.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
    @Autowired private StringRedisTemplate redisTemplate;

    public boolean isRateLimited(String key, int maxAttempts, int windowSeconds) {
        String count = redisTemplate.opsForValue().get("ratelimit:" + key);
        if (count == null) {
            redisTemplate.opsForValue().set("ratelimit:" + key, "1", java.time.Duration.ofSeconds(windowSeconds));
            return false;
        }
        return Integer.parseInt(count) >= maxAttempts;
    }

    public void increment(String key, int windowSeconds) {
        String redisKey = "ratelimit:" + key;
        Long newCount = redisTemplate.opsForValue().increment(redisKey);
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(redisKey, java.time.Duration.ofSeconds(windowSeconds));
        }
    }
}
