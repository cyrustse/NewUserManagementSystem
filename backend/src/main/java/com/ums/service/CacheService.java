package com.ums.service;

import java.util.Optional;

public interface CacheService {
    
    <T> void set(String key, T value, long ttlSeconds);
    
    <T> Optional<T> get(String key, Class<T> type);
    
    void delete(String key);
    
    void deleteByPattern(String pattern);
    
    boolean exists(String key);
}
