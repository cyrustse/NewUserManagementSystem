package com.ums.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.isAccessToken(token)) {
                var claims = jwtTokenProvider.validateToken(token);
                String userId = claims.getSubject();
                List<String> roles = claims.get("roles", List.class);
                
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                
                if (roles != null) {
                    for (String role : roles) {
                        // Add role authority with ROLE_ prefix
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        
                        // Add permission authorities based on role
                        if ("SUPER_ADMIN".equals(role)) {
                            // SUPER_ADMIN has all permissions
                            authorities.add(new SimpleGrantedAuthority("user:create"));
                            authorities.add(new SimpleGrantedAuthority("user:read"));
                            authorities.add(new SimpleGrantedAuthority("user:update"));
                            authorities.add(new SimpleGrantedAuthority("user:delete"));
                            authorities.add(new SimpleGrantedAuthority("role:create"));
                            authorities.add(new SimpleGrantedAuthority("role:read"));
                            authorities.add(new SimpleGrantedAuthority("role:update"));
                            authorities.add(new SimpleGrantedAuthority("role:delete"));
                            authorities.add(new SimpleGrantedAuthority("permission:create"));
                            authorities.add(new SimpleGrantedAuthority("permission:read"));
                            authorities.add(new SimpleGrantedAuthority("permission:update"));
                            authorities.add(new SimpleGrantedAuthority("permission:delete"));
                            authorities.add(new SimpleGrantedAuthority("audit:read"));
                        } else if ("USER_ADMIN".equals(role)) {
                            authorities.add(new SimpleGrantedAuthority("user:create"));
                            authorities.add(new SimpleGrantedAuthority("user:read"));
                            authorities.add(new SimpleGrantedAuthority("user:update"));
                            authorities.add(new SimpleGrantedAuthority("user:delete"));
                        } else if ("USER".equals(role)) {
                            authorities.add(new SimpleGrantedAuthority("user:read"));
                        }
                    }
                }
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Token validation failed, continue without authentication
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        // First try Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Fall back to accessToken cookie
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
