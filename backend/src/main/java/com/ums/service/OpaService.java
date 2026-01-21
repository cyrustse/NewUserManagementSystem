package com.ums.service;

import java.util.Map;

public interface OpaService {
    
    boolean evaluate(String userId, String resource, String action, Map<String, Object> context);
    
    void invalidateCache(String userId);
    
    void refreshOpaData();
}
