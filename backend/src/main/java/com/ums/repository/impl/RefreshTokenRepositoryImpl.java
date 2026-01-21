package com.ums.repository.impl;

import com.ums.entity.RefreshToken;
import com.ums.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    private final RowMapper<RefreshToken> refreshTokenRowMapper = (rs, rowNum) -> {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.fromString(rs.getString("id")));
        token.setUserId(UUID.fromString(rs.getString("user_id")));
        token.setTokenHash(rs.getString("token_hash"));
        
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            token.setExpiresAt(expiresAt.toLocalDateTime());
        }
        
        Timestamp revokedAt = rs.getTimestamp("revoked_at");
        if (revokedAt != null) {
            token.setRevokedAt(revokedAt.toLocalDateTime());
        }
        
        token.setIpAddress(rs.getString("ip_address"));
        token.setUserAgent(rs.getString("user_agent"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            token.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return token;
    };
    
    @Override
    public Optional<RefreshToken> findById(UUID id) {
        String sql = "SELECT * FROM \"refresh_tokens\" WHERE id = ?";
        List<RefreshToken> tokens = jdbcTemplate.query(sql, refreshTokenRowMapper, id);
        return tokens.isEmpty() ? Optional.empty() : Optional.of(tokens.get(0));
    }
    
    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        String sql = "SELECT * FROM \"refresh_tokens\" WHERE token_hash = ?";
        List<RefreshToken> tokens = jdbcTemplate.query(sql, refreshTokenRowMapper, tokenHash);
        return tokens.isEmpty() ? Optional.empty() : Optional.of(tokens.get(0));
    }
    
    @Override
    public Optional<RefreshToken> findByUserId(UUID userId) {
        String sql = "SELECT * FROM \"refresh_tokens\" WHERE user_id = ? AND revoked_at IS NULL ORDER BY created_at DESC LIMIT 1";
        List<RefreshToken> tokens = jdbcTemplate.query(sql, refreshTokenRowMapper, userId);
        return tokens.isEmpty() ? Optional.empty() : Optional.of(tokens.get(0));
    }
    
    @Override
    public RefreshToken save(RefreshToken token) {
        if (token.getId() == null) {
            UUID id = UUID.randomUUID();
            token.setId(id);

            // Build INSERT with literal values to avoid parameterization issues
            String sql = "INSERT INTO \"refresh_tokens\" (id, user_id, token_hash, expires_at, revoked_at, ip_address, user_agent, created_at) " +
                        "VALUES ('" + id.toString() + "', '" + token.getUserId().toString() + "', '" + token.getTokenHash().replace("'", "''") + "', " +
                        (token.getExpiresAt() != null ? "'" + token.getExpiresAt().toString() + "'" : "NULL") + ", " +
                        (token.getRevokedAt() != null ? "'" + token.getRevokedAt().toString() + "'" : "NULL") + ", " +
                        (token.getIpAddress() != null && !token.getIpAddress().isEmpty() ? "'" + token.getIpAddress().replace("'", "''") + "'" : "NULL") + ", " +
                        "'" + (token.getUserAgent() != null ? token.getUserAgent().replace("'", "''") : "") + "', " +
                        "'" + LocalDateTime.now().toString() + "')";

            jdbcTemplate.execute(sql);
        } else {
            // Build UPDATE with literal values to avoid parameterization issues
            StringBuilder sql = new StringBuilder("UPDATE \"refresh_tokens\" SET ");
            sql.append("token_hash = '").append(token.getTokenHash().replace("'", "''")).append("'");
            if (token.getExpiresAt() != null) {
                sql.append(", expires_at = '").append(token.getExpiresAt().toString()).append("'");
            } else {
                sql.append(", expires_at = NULL");
            }
            if (token.getRevokedAt() != null) {
                sql.append(", revoked_at = '").append(token.getRevokedAt().toString()).append("'");
            } else {
                sql.append(", revoked_at = NULL");
            }
            if (token.getUserAgent() != null) {
                sql.append(", user_agent = '").append(token.getUserAgent().replace("'", "''")).append("'");
            } else {
                sql.append(", user_agent = ''");
            }
            if (token.getIpAddress() != null) {
                sql.append(", ip_address = '").append(token.getIpAddress().replace("'", "''")).append("'");
            }
            sql.append(" WHERE id = '").append(token.getId().toString()).append("'");

            jdbcTemplate.execute(sql.toString());
        }
        return token;
    }
    
    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM \"refresh_tokens\" WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    @Override
    public void revokeAllByUserId(UUID userId) {
        String sql = "UPDATE \"refresh_tokens\" SET revoked_at = ? WHERE user_id = ?";
        jdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.now()), userId);
    }
}
