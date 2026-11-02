package com.example.backend.notification;

import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.repository.UpcomingTurnoAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class UpcomingTurnoReminderEnqueuer {

    private static final Logger logger = LoggerFactory.getLogger(UpcomingTurnoReminderEnqueuer.class);
    private static final DateTimeFormatter TURN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TurnoRepository turnoRepository;
    private final NotificationOutboxService outboxService;
    private final Clock clock;
    private final long reminderWindowHours;

    public UpcomingTurnoReminderEnqueuer(
            TurnoRepository turnoRepository,
            NotificationOutboxService outboxService,
            Clock clock,
            @Value("${app.notification.reminders.window-hours:24}") long reminderWindowHours) {
        if (reminderWindowHours < 1) {
            throw new IllegalArgumentException("The shift reminder window must be positive.");
        }
        this.turnoRepository = turnoRepository;
        this.outboxService = outboxService;
        this.clock = clock;
        this.reminderWindowHours = reminderWindowHours;
    }

    @Scheduled(fixedDelayString = "${app.notification.reminders.poll-interval-ms:900000}")
    public void enqueueUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (UpcomingTurnoAssignment assignment : turnoRepository.findUpcomingAssignments(
                now,
                now.plusHours(reminderWindowHours))) {
            String firstName = assignment.getNombre() == null ? "" : assignment.getNombre().trim();
            String lastName = assignment.getApellido() == null ? "" : assignment.getApellido().trim();
            String name = (firstName + " " + lastName).trim();
            if (name.isBlank()) {
                logger.warn("Skipping shift reminder without recipient name: turno={}, voluntario={}",
                        assignment.getIdTurno(), assignment.getIdVoluntario());
                continue;
            }
            String date = LocalDateTime.of(assignment.getFecha(), assignment.getHoraInicio())
                    .format(TURN_DATE_FORMAT);
            String idempotencyKey = "shift-reminder:"
                    + assignment.getIdTurno() + ":"
                    + assignment.getIdVoluntario() + ":"
                    + assignment.getFecha();
            outboxService.enqueue(
                    idempotencyKey,
                    NotificationTemplateType.RECORDATORIO,
                    assignment.getEmail().trim(),
                    name,
                    assignment.getActividad(),
                    date);
        }
    }
}
