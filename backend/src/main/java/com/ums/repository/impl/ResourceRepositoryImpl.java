package com.ums.repository.impl;

import com.ums.entity.Resource;
import com.ums.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ResourceRepositoryImpl implements ResourceRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Resource> resourceRowMapper = (rs, rowNum) -> {
        Resource resource = new Resource();
        resource.setId(UUID.fromString(rs.getString("id")));
        resource.setName(rs.getString("name"));
        resource.setDescription(rs.getString("description"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            resource.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            resource.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return resource;
    };

    @Override
    public Optional<Resource> findById(UUID id) {
        String sql = "SELECT id::text, name, description, created_at, updated_at FROM \"resources\" WHERE id = ?";
        List<Resource> resources = jdbcTemplate.query(sql, resourceRowMapper, id);
        return resources.isEmpty() ? Optional.empty() : Optional.of(resources.get(0));
    }

    @Override
    public Optional<Resource> findByName(String name) {
        String sql = "SELECT id::text, name, description, created_at, updated_at FROM \"resources\" WHERE name = ?";
        List<Resource> resources = jdbcTemplate.query(sql, resourceRowMapper, name);
        return resources.isEmpty() ? Optional.empty() : Optional.of(resources.get(0));
    }

    @Override
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM \"resources\" WHERE name = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, name) > 0;
    }

    @Override
    public List<Resource> findAll() {
        String sql = "SELECT id::text, name, description, created_at, updated_at FROM \"resources\" ORDER BY name";
        return jdbcTemplate.query(sql, resourceRowMapper);
    }

    @Override
    public Resource save(Resource resource) {
        if (resource.getId() == null) {
            String sql = "INSERT INTO \"resources\" (id, name, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
            UUID id = UUID.randomUUID();
            resource.setId(id);
            jdbcTemplate.update(sql,
                id,
                resource.getName(),
                resource.getDescription(),
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        } else {
            String sql = "UPDATE \"resources\" SET name = ?, description = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                resource.getName(),
                resource.getDescription(),
                LocalDateTime.now(),
                resource.getId()
            );
        }
        return resource;
    }

    @Override
    public void deleteById(UUID id) {
        String sql = "DELETE FROM \"resources\" WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
