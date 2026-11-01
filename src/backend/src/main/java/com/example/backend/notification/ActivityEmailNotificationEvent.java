package com.example.backend.notification;

import com.example.backend.mail.NotificationTemplateType;

public record ActivityEmailNotificationEvent(
        NotificationTemplateType templateType,
        String recipient,
        String name,
        String activity,
        String date) {
}
