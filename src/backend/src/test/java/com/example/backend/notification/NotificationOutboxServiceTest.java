package com.example.backend.notification;

import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.NotificationOutboxMessage;
import com.example.backend.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationOutboxServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-25T10:00:00Z");
    private final NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
    
    // HU1: Inicializamos el servicio con el nuevo valor por defecto (3 intentos máximos configurables)
    private final NotificationOutboxService service = new NotificationOutboxService(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            3, // maxAttempts
            20, // batchSize
            300, // leaseSeconds
            30); // retryBaseSeconds

    @Test
    void enqueueUsesUniqueEventKeyAndTemplateData() {
        service.enqueue(
                "task-assignment:3:9",
                NotificationTemplateType.ASIGNACION,
                "ana@example.com",
                "Ana Paz",
                "Inventario",
                "25/04/2026 10:00");

        verify(repository).enqueueIfAbsent(
                "task-assignment:3:9",
                "ASIGNACION",
                "ana@example.com",
                "Ana Paz",
                "Inventario",
                "25/04/2026 10:00");
    }

    @Test
    void claimsBatchWithLeaseAndUniqueClaimToken() {
        NotificationOutboxMessage message = NotificationOutboxMessage.builder().id(1L).build();
        when(repository.findAllByClaimTokenOrderByCreatedAtAsc(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(message));

        List<NotificationOutboxMessage> claimed = service.claimDueMessages();

        assertEquals(List.of(message), claimed);
        org.mockito.ArgumentCaptor<String> token = org.mockito.ArgumentCaptor.forClass(String.class);
        
        // Verificamos que se usan los 3 intentos máximos de la configuración de HU1
        verify(repository).failExhaustedClaims(
                org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 4, 25, 9, 55)));
        
        verify(repository).claimDue(
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 4, 25, 10, 0)),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 4, 25, 9, 55)),
                org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.eq(20),
                token.capture());
                
        verify(repository).findAllByClaimTokenOrderByCreatedAtAsc(token.getValue());
    }

    @Test
    void schedulesExponentialRetryAndTruncatesNoDataByDefault() {
        NotificationOutboxMessage message = NotificationOutboxMessage.builder()
                .id(1L)
                .attemptCount(2)
                .claimToken("claim-1")
                .build();

        service.retry(message, "CONNECTION");

        // HU1: Verifica el cálculo del backoff para el siguiente reintento
        verify(repository).scheduleRetry(
                1L,
                "claim-1",
                LocalDateTime.of(2026, 4, 25, 10, 1),
                "CONNECTION");
    }
}