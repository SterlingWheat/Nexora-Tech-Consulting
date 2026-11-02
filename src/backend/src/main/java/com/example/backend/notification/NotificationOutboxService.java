package com.example.backend.notification;

import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.NotificationOutboxMessage;
import com.example.backend.repository.NotificationOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationOutboxService {

    private final NotificationOutboxRepository repository;
    private final Clock clock;
    private final int maxAttempts;
    private final int batchSize;
    private final Duration leaseDuration;
    private final Duration retryBaseDelay;

    public NotificationOutboxService(
            NotificationOutboxRepository repository,
            Clock clock,
            @Value("${app.notification.outbox.max-attempts:5}") int maxAttempts,
            @Value("${app.notification.outbox.batch-size:20}") int batchSize,
            @Value("${app.notification.outbox.lease-seconds:300}") long leaseSeconds,
            @Value("${app.notification.outbox.retry-base-seconds:30}") long retryBaseSeconds) {
        if (maxAttempts < 1 || batchSize < 1 || leaseSeconds < 1 || retryBaseSeconds < 1) {
            throw new IllegalArgumentException("Notification outbox settings must be positive.");
        }
        this.repository = repository;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
        this.leaseDuration = Duration.ofSeconds(leaseSeconds);
        this.retryBaseDelay = Duration.ofSeconds(retryBaseSeconds);
    }

    @Transactional
    public void enqueue(
            String idempotencyKey,
            NotificationTemplateType templateType,
            String recipient,
            String recipientName,
            String activity,
            String activityDate) {
        repository.enqueueIfAbsent(
                idempotencyKey,
                templateType.name(),
                recipient,
                recipientName,
                activity,
                activityDate);
    }

    @Transactional
    public List<NotificationOutboxMessage> claimDueMessages() {
        LocalDateTime now = LocalDateTime.now(clock);
        String claimToken = UUID.randomUUID().toString();
        repository.failExhaustedClaims(maxAttempts, now.minus(leaseDuration));
        repository.claimDue(
                now,
                now.minus(leaseDuration),
                maxAttempts,
                batchSize,
                claimToken);
        return repository.findAllByClaimTokenOrderByCreatedAtAsc(claimToken);
    }

    @Transactional
    public void markSent(NotificationOutboxMessage message) {
        repository.markSent(message.getId(), message.getClaimToken(), LocalDateTime.now(clock));
    }

    @Transactional
    public void retry(NotificationOutboxMessage message, String safeReason) {
        if (message.getAttemptCount() >= maxAttempts) {
            fail(message, safeReason);
            return;
        }
        long multiplier = 1L << Math.min(Math.max(message.getAttemptCount() - 1, 0), 20);
        long delaySeconds = Math.min(
                retryBaseDelay.multipliedBy(multiplier).toSeconds(),
                Duration.ofHours(1).toSeconds());
        repository.scheduleRetry(
                message.getId(),
                message.getClaimToken(),
                LocalDateTime.now(clock).plusSeconds(delaySeconds),
                truncate(safeReason));
    }

    @Transactional
    public void fail(NotificationOutboxMessage message, String safeReason) {
        repository.markFailed(message.getId(), message.getClaimToken(), truncate(safeReason));
    }

    private String truncate(String value) {
        if (value == null) {
            return "Notification delivery failed without an error message.";
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
