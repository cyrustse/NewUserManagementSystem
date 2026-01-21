package com.ums.service.impl;

import com.ums.entity.Permission;
import com.ums.entity.Role;
import com.ums.entity.RolePermission;
import com.ums.repository.PermissionRepository;
import com.ums.repository.RolePermissionRepository;
import com.ums.repository.RoleRepository;
import com.ums.service.OpaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OpaServiceImpl implements OpaService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    @Lazy
    private WebClient.Builder webClientBuilder;

    @Value("${opa.url:http://localhost:8181}")
    private String opaUrl;

    private final Map<String, Boolean> decisionCache = new ConcurrentHashMap<>();

    private WebClient getWebClient() {
        return webClientBuilder.build();
    }

    @Override
    public boolean evaluate(String userId, String resource, String action, Map<String, Object> context) {
        // Check cache first
        String cacheKey = userId + ":" + resource + ":" + action;
        if (decisionCache.containsKey(cacheKey)) {
            return decisionCache.get(cacheKey);
        }

        // Build OPA input
        Map<String, Object> input = new HashMap<>();
        input.put("user_id", userId);
        input.put("resource", resource);
        input.put("action", action);
        input.put("context", context);

        Map<String, Object> opaRequest = new HashMap<>();
        opaRequest.put("input", input);

        try {
            OpaResponse response = getWebClient().post()
                    .uri(opaUrl + "/v1/data/user_management/authz")
                    .bodyValue(opaRequest)
                    .retrieve()
                    .bodyToMono(OpaResponse.class)
                    .block();

            boolean allowed = response != null && response.getResult() != null && response.getResult().isAllow();
            decisionCache.put(cacheKey, allowed);
            return allowed;
        } catch (Exception e) {
            // Log error and fail open (allow) for safety in development
            // In production, you might want to fail closed (deny)
            System.err.println("OPA evaluation failed: " + e.getMessage());
            return true; // Fail open
        }
    }

    @Override
    public void invalidateCache(String userId) {
        // Remove cache entries for this user
        decisionCache.entrySet().removeIf(entry -> entry.getKey().startsWith(userId + ":"));
    }

    @Override
    public void refreshOpaData() {
        List<Role> roles = roleRepository.findAllActive();

        List<Permission> permissions = permissionRepository.findAllActive();

        List<RolePermission> rolePermissions = rolePermissionRepository.findAll();

        Map<String, Object> opaData = new HashMap<>();
        opaData.put("roles", roles);
        opaData.put("permissions", permissions);
        opaData.put("role_permissions", rolePermissions);

        try {
            getWebClient().post()
                    .uri(opaUrl + "/v1/data")
                    .bodyValue(opaData)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to refresh OPA data: " + e.getMessage());
        }
    }

    /**
     * Inner class for OPA response structure
     */
    private static class OpaResponse {
        private OpaResult result;

        public OpaResult getResult() {
            return result;
        }

        public void setResult(OpaResult result) {
            this.result = result;
        }

        private static class OpaResult {
            private boolean allow;

            public boolean isAllow() {
                return allow;
            }

            public void setAllow(boolean allow) {
                this.allow = allow;
            }
        }
    }
}
