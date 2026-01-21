package com.ums.repository;

import com.ums.entity.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    
    Optional<RefreshToken> findById(UUID id);
    
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    Optional<RefreshToken> findByUserId(UUID userId);
    
    RefreshToken save(RefreshToken token);
    
    void deleteById(UUID id);
    
    void revokeAllByUserId(UUID userId);
}
