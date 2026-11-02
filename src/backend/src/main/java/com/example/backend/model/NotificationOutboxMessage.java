package com.example.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_outbox", indexes = {
        @Index(name = "idx_notification_outbox_due", columnList = "status,next_attempt_at,created_at"),
        @Index(name = "idx_notification_outbox_claim", columnList = "claim_token")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(name = "template_type", nullable = false, length = 30)
    private String templateType;

    @Column(nullable = false, length = 254)
    private String recipient;

    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Column(nullable = false, length = 500)
    private String activity;

    @Column(name = "activity_date", nullable = false, length = 100)
    private String activityDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
