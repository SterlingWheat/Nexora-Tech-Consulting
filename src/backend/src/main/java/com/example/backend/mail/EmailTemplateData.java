package com.example.backend.mail;

import java.time.LocalDateTime;

public record EmailTemplateData(
        String recipientName,
        String recipientEmail,
        String details,
        String actionUrl,
        String actionLabel,
        LocalDateTime expiresAt) {
}
