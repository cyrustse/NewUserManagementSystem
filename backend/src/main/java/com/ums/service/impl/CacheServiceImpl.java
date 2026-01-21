package com.ums.service.impl;

import com.ums.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
public class CacheServiceImpl implements CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheServiceImpl.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Value("${spring.redis.cache.ttl:3600}")
    private long defaultTtlSeconds;
    
    @Override
    public <T> void set(String key, T value, long ttlSeconds) {
        try {
            Duration ttl = Duration.ofSeconds(ttlSeconds > 0 ? ttlSeconds : defaultTtlSeconds);
            redisTemplate.opsForValue().set(key, value, ttl);
            logger.debug("Cache set for key: {} with TTL: {} seconds", key, ttlSeconds);
        } catch (Exception e) {
            logger.error("Failed to set cache for key: {}", key, e);
        }
    }
    
    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isInstance(value)) {
                logger.debug("Cache hit for key: {}", key);
                return Optional.of(type.cast(value));
            }
            logger.debug("Cache miss for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to get cache for key: {}", key, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Cache deleted for key: {}", key);
            }
        } catch (Exception e) {
            logger.error("Failed to delete cache for key: {}", key, e);
        }
    }
    
    @Override
    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                logger.debug("Cache deleted {} keys matching pattern: {}", deleted, pattern);
            }
        } catch (Exception e) {
            logger.error("Failed to delete cache by pattern: {}", pattern, e);
        }
    }
    
    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.error("Failed to check cache existence for key: {}", key, e);
            return false;
        }
    }
}
