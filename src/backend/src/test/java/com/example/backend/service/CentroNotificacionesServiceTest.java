package com.example.backend.service;

import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.NotificationOutboxMessage;
import com.example.backend.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CentroNotificacionesServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-21T20:00:00Z"), ZoneOffset.UTC);

    private final NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
    private final CentroNotificacionesService service =
            new CentroNotificacionesService(repository, CLOCK);

    @Test
    void listarDevuelveAvisosEnElOrdenMasRecientePrimero() {
        List<NotificationOutboxMessage> sent = List.of(
                message(2L, "25/11/2026 10:00", LocalDateTime.of(2026, 11, 21, 19, 30), null),
                message(1L, "24/11/2026 10:00", LocalDateTime.of(2026, 11, 21, 18, 30), null));
        when(repository.findAllByRecipientIgnoreCaseAndStatusOrderByCreatedAtDesc(
                "voluntario@example.com", "SENT")).thenReturn(sent);

        var result = service.listar("voluntario@example.com");

        assertEquals(List.of(2L, 1L), result.stream().map(item -> item.id()).toList());
        assertEquals("Asignación", result.get(0).tipo());
        assertFalse(result.get(0).leida());
        verify(repository).findAllByRecipientIgnoreCaseAndStatusOrderByCreatedAtDesc(
                "voluntario@example.com", "SENT");
    }

    @Test
    void marcarLeidaPersisteFechaDeLectura() {
        NotificationOutboxMessage sent = message(
                7L, "25/11/2026 10:00", LocalDateTime.of(2026, 11, 21, 19, 30), null);
        when(repository.findByIdAndRecipientIgnoreCaseAndStatus(
                7L, "voluntario@example.com", "SENT")).thenReturn(Optional.of(sent));
        when(repository.markAsRead(7L, "voluntario@example.com",
                LocalDateTime.of(2026, 11, 21, 20, 0))).thenReturn(1);

        service.marcarLeida(7L, "voluntario@example.com");

        verify(repository).markAsRead(
                7L, "voluntario@example.com", LocalDateTime.of(2026, 11, 21, 20, 0));
    }

    @Test
    void marcarLeidaEsIdempotenteParaAvisoYaLeido() {
        NotificationOutboxMessage sent = message(
                7L,
                "25/11/2026 10:00",
                LocalDateTime.of(2026, 11, 21, 19, 30),
                LocalDateTime.of(2026, 11, 21, 19, 45));
        when(repository.findByIdAndRecipientIgnoreCaseAndStatus(
                7L, "voluntario@example.com", "SENT")).thenReturn(Optional.of(sent));

        service.marcarLeida(7L, "voluntario@example.com");

        verify(repository, never()).markAsRead(
                7L, "voluntario@example.com", LocalDateTime.of(2026, 11, 21, 20, 0));
        assertTrue(service.listar("voluntario@example.com").isEmpty());
    }

    @Test
    void noPermiteMarcarLeidoUnAvisoDeOtroUsuario() {
        when(repository.findByIdAndRecipientIgnoreCaseAndStatus(
                7L, "voluntario@example.com", "SENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.marcarLeida(7L, "voluntario@example.com"));
    }

    private NotificationOutboxMessage message(
            Long id, String activityDate, LocalDateTime createdAt, LocalDateTime readAt) {
        return NotificationOutboxMessage.builder()
                .id(id)
                .templateType("ASIGNACION")
                .recipient("voluntario@example.com")
                .activity("Apoyar en la actividad")
                .activityDate(activityDate)
                .status("SENT")
                .createdAt(createdAt)
                .sentAt(createdAt)
                .readAt(readAt)
                .build();
    }
}
