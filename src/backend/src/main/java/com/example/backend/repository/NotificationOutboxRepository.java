package com.example.backend.repository;

import com.example.backend.model.NotificationOutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxMessage, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO notification_outbox
                (idempotency_key, template_type, recipient, recipient_name, activity, activity_date,
                 status, attempt_count, created_at, next_attempt_at)
            VALUES
                (:idempotencyKey, :templateType, :recipient, :recipientName, :activity, :activityDate,
                 'PENDING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int enqueueIfAbsent(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("templateType") String templateType,
            @Param("recipient") String recipient,
            @Param("recipientName") String recipientName,
            @Param("activity") String activity,
            @Param("activityDate") String activityDate);

    @Modifying
    @Query(value = """
            WITH candidates AS (
                SELECT id
                FROM notification_outbox
                WHERE attempt_count < :maxAttempts
                  AND (
                    (status IN ('PENDING', 'RETRY') AND next_attempt_at <= :now)
                    OR (status = 'PROCESSING' AND locked_at < :leaseExpiresBefore)
                  )
                ORDER BY created_at, id
                FOR UPDATE SKIP LOCKED
                LIMIT :batchSize
            )
            UPDATE notification_outbox AS message
            SET status = 'PROCESSING',
                attempt_count = message.attempt_count + 1,
                locked_at = :now,
                claim_token = :claimToken
            FROM candidates
            WHERE message.id = candidates.id
            """, nativeQuery = true)
    int claimDue(
            @Param("now") LocalDateTime now,
            @Param("leaseExpiresBefore") LocalDateTime leaseExpiresBefore,
            @Param("maxAttempts") int maxAttempts,
            @Param("batchSize") int batchSize,
            @Param("claimToken") String claimToken);

    @Modifying
    @Query("""
            UPDATE NotificationOutboxMessage message
            SET message.status = 'FAILED',
                message.lockedAt = null,
                message.claimToken = null,
                message.lastError = 'Worker lease expired after maximum delivery attempts'
            WHERE message.status = 'PROCESSING'
              AND message.attemptCount >= :maxAttempts
              AND message.lockedAt < :leaseExpiresBefore
            """)
    int failExhaustedClaims(
            @Param("maxAttempts") int maxAttempts,
            @Param("leaseExpiresBefore") LocalDateTime leaseExpiresBefore);

    List<NotificationOutboxMessage> findAllByClaimTokenOrderByCreatedAtAsc(String claimToken);

    @Modifying
    @Query("""
            UPDATE NotificationOutboxMessage message
            SET message.status = 'SENT',
                message.sentAt = :sentAt,
                message.lockedAt = null,
                message.claimToken = null,
                message.lastError = null
            WHERE message.id = :id AND message.claimToken = :claimToken
            """)
    int markSent(
            @Param("id") Long id,
            @Param("claimToken") String claimToken,
            @Param("sentAt") LocalDateTime sentAt);

    @Modifying
    @Query("""
            UPDATE NotificationOutboxMessage message
            SET message.status = 'RETRY',
                message.nextAttemptAt = :nextAttemptAt,
                message.lockedAt = null,
                message.claimToken = null,
                message.lastError = :lastError
            WHERE message.id = :id AND message.claimToken = :claimToken
            """)
    int scheduleRetry(
            @Param("id") Long id,
            @Param("claimToken") String claimToken,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("lastError") String lastError);

    @Modifying
    @Query("""
            UPDATE NotificationOutboxMessage message
            SET message.status = 'FAILED',
                message.lockedAt = null,
                message.claimToken = null,
                message.lastError = :lastError
            WHERE message.id = :id AND message.claimToken = :claimToken
            """)
    int markFailed(
            @Param("id") Long id,
            @Param("claimToken") String claimToken,
            @Param("lastError") String lastError);
}
