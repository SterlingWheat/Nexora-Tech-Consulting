package com.example.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_rate_limit")
public class PasswordResetRateLimit {

    @Id
    @Column(name = "email_fingerprint", length = 64)
    private String emailFingerprint;

    @Column(name = "ultima_solicitud", nullable = false)
    private LocalDateTime ultimaSolicitud;

    protected PasswordResetRateLimit() {
    }
}
