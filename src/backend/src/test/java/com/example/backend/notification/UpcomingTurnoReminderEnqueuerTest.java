package com.example.backend.notification;

import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.repository.UpcomingTurnoAssignment;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

class UpcomingTurnoReminderEnqueuerTest {

    @Test
    void enqueuesReminderForAssignedVolunteerWithinTheConfiguredWindow() {
        TurnoRepository turnoRepository = mock(TurnoRepository.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 4, 25, 10, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC);
        UpcomingTurnoAssignment assignment = new UpcomingTurnoAssignment() {
            @Override
            public Integer getIdTurno() {
                return 8;
            }

            @Override
            public Integer getIdVoluntario() {
                return 15;
            }

            @Override
            public String getNombre() {
                return "Ana";
            }

            @Override
            public String getApellido() {
                return "Paz";
            }

            @Override
            public String getEmail() {
                return " ana@example.com ";
            }

            @Override
            public String getActividad() {
                return "Clasificación";
            }

            @Override
            public LocalDate getFecha() {
                return LocalDate.of(2026, 4, 26);
            }

            @Override
            public LocalTime getHoraInicio() {
                return LocalTime.of(9, 0);
            }
        };
        when(turnoRepository.findUpcomingAssignments(
                LocalDateTime.of(2026, 4, 25, 10, 0),
                LocalDateTime.of(2026, 4, 26, 10, 0)))
                .thenReturn(List.of(assignment));

        UpcomingTurnoReminderEnqueuer enqueuer = new UpcomingTurnoReminderEnqueuer(
                turnoRepository,
                outboxService,
                clock,
                24);
        enqueuer.enqueueUpcomingReminders();

        verify(outboxService).enqueue(
                eq("shift-reminder:8:15:2026-04-26"),
                eq(NotificationTemplateType.RECORDATORIO),
                eq("ana@example.com"),
                eq("Ana Paz"),
                eq("Clasificación"),
                eq("26/04/2026 09:00"));
    }
}
