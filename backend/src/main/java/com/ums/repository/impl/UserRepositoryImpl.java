package com.ums.repository.impl;

import com.ums.entity.User;
import com.ums.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

    @Repository
public class UserRepositoryImpl implements UserRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryImpl.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    private Timestamp toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Timestamp.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(UUID.fromString(rs.getString("id")));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPhone(rs.getString("phone"));
        
        String status = rs.getString("status");
        if (status != null) {
            user.setStatus(User.UserStatus.valueOf(status));
        }
        
        user.setMfaEnabled(rs.getBoolean("mfa_enabled"));
        user.setMfaSecret(rs.getString("mfa_secret"));
        
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        if (lastLogin != null) {
            user.setLastLoginAt(lastLogin.toLocalDateTime());
        }
        
        user.setLoginAttempts(rs.getInt("login_attempts"));
        
        Timestamp lockedUntil = rs.getTimestamp("locked_until");
        if (lockedUntil != null) {
            user.setLockedUntil(lockedUntil.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) {
            user.setDeletedAt(deletedAt.toLocalDateTime());
        }
        
        return user;
    };
    
    @Override
    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM \"users\" WHERE id = ? AND deleted_at IS NULL";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM \"users\" WHERE email = ? AND deleted_at IS NULL";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, email);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM \"users\" WHERE username = ? AND deleted_at IS NULL";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, username);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    @Override
    public List<User> findAll(int page, int size, String status, String search) {
        StringBuilder sql = new StringBuilder("SELECT * FROM \"users\" WHERE deleted_at IS NULL");
        List<Object> params = new ArrayList<>();
        
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (username ILIKE ? OR email ILIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }
        
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);
        
        return jdbcTemplate.query(sql.toString(), userRowMapper, params.toArray());
    }
    
    @Override
    public long count(String status, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM \"users\" WHERE deleted_at IS NULL");
        List<Object> params = new ArrayList<>();
        
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (username ILIKE ? OR email ILIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }
        
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    }
    
    @Override
    public User save(User user) {
        if (user.getId() == null) {
            // Insert new user - handle null values properly
            String sql = "INSERT INTO \"users\" (id, username, email, password_hash, phone, \"status\", mfa_enabled, mfa_secret, login_attempts, created_at, updated_at) " +
                        "VALUES (:id, :username, :email, :passwordHash, :phone, :status::user_status, :mfaEnabled, :mfaSecret, :loginAttempts, NOW(), NOW())";
            UUID id = UUID.randomUUID();
            user.setId(id);

            java.util.HashMap<String, Object> params = new java.util.HashMap<>();
            params.put("id", id);
            params.put("username", user.getUsername());
            params.put("email", user.getEmail());
            params.put("passwordHash", user.getPasswordHash());
            params.put("phone", user.getPhone() != null ? user.getPhone() : null);
            params.put("status", user.getStatus() != null ? user.getStatus().name() : "PENDING");
            params.put("mfaEnabled", user.isMfaEnabled());
            params.put("mfaSecret", user.getMfaSecret() != null ? user.getMfaSecret() : null);
            params.put("loginAttempts", user.getLoginAttempts());

            namedParameterJdbcTemplate.update(sql, params);
        } else {
            // Update existing user - build dynamic SQL for fields that are set
            StringBuilder sql = new StringBuilder("UPDATE \"users\" SET updated_at = NOW()");

            java.util.HashMap<String, Object> params = new java.util.HashMap<>();
            params.put("id", user.getId());

            if (user.getUsername() != null) {
                sql.append(", username = :username");
                params.put("username", user.getUsername());
            }
            if (user.getEmail() != null) {
                sql.append(", email = :email");
                params.put("email", user.getEmail());
            }
            if (user.getPasswordHash() != null) {
                sql.append(", password_hash = :passwordHash");
                params.put("passwordHash", user.getPasswordHash());
            }
            if (user.getPhone() != null) {
                sql.append(", phone = :phone");
                params.put("phone", user.getPhone());
            }
            if (user.getStatus() != null) {
                sql.append(", status = :status::user_status");
                params.put("status", user.getStatus().name());
            }
            if (user.getMfaSecret() != null) {
                sql.append(", mfa_secret = :mfaSecret");
                params.put("mfaSecret", user.getMfaSecret());
            }
            if (user.getLastLoginAt() != null) {
                sql.append(", last_login_at = :lastLoginAt");
                params.put("lastLoginAt", toTimestamp(user.getLastLoginAt()));
            }
            if (user.getLockedUntil() != null) {
                sql.append(", locked_until = :lockedUntil");
                params.put("lockedUntil", toTimestamp(user.getLockedUntil()));
            }

            sql.append(" WHERE id = :id");

            String finalSql = sql.toString();
            logger.info("DEBUG_SQL: {}", finalSql);
            logger.info("DEBUG_PARAMS: {}", params);
            try {
                namedParameterJdbcTemplate.update(finalSql, params);
                logger.info("UPDATE completed successfully");
            } catch (Exception e) {
                logger.error("UPDATE FAILED: " + e.getMessage(), e);
                throw e;
            }
        }
        return user;
    }
    
    @Override
    public void deleteById(UUID id) {
        String sql = "UPDATE \"users\" SET deleted_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), id);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM \"users\" WHERE email = ? AND deleted_at IS NULL";
        return jdbcTemplate.queryForObject(sql, Long.class, email) > 0;
    }
    
    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM \"users\" WHERE username = ? AND deleted_at IS NULL";
        return jdbcTemplate.queryForObject(sql, Long.class, username) > 0;
    }
}
