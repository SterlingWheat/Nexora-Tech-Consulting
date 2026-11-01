package com.example.backend.dto;

import java.time.LocalDateTime;

public record NotificationTemplateResponse(
        String templateType,
        String name,
        String subject,
        String htmlBody,
        String textBody,
        boolean active,
        LocalDateTime updatedAt) {
}
