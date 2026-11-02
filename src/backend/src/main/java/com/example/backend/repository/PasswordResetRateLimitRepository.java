package com.example.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class PasswordResetRateLimitRepository {

    private static final String ACQUIRE_REQUEST = """
            INSERT INTO password_reset_rate_limit (email_fingerprint, ultima_solicitud)
            VALUES (?, ?)
            ON CONFLICT (email_fingerprint) DO UPDATE
            SET ultima_solicitud = EXCLUDED.ultima_solicitud
            WHERE password_reset_rate_limit.ultima_solicitud <= ?
            RETURNING email_fingerprint
            """;

    private final JdbcTemplate jdbcTemplate;

    public PasswordResetRateLimitRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tryAcquire(
            String emailFingerprint,
            LocalDateTime now,
            LocalDateTime earliestAllowed) {
        return !jdbcTemplate.query(
                ACQUIRE_REQUEST,
                (resultSet, rowNumber) -> resultSet.getString(1),
                emailFingerprint,
                now,
                earliestAllowed).isEmpty();
    }
}
