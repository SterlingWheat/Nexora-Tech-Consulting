package com.example.backend.notification;

public record AccountEmailNotificationEvent(
        Kind kind,
        String recipient,
        String recipientName,
        String details) {

    public enum Kind {
        WELCOME,
        ACCOUNT_CHANGE,
        PASSWORD_RESET
    }
}
