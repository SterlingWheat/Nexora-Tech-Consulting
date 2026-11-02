package com.example.backend.notification;

import com.example.backend.dto.NotificationEmailRequest;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.SmtpEmailException;
import com.example.backend.mail.NotificationTemplateService;
import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.NotificationOutboxMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationOutboxWorkerTest {

    private final NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
    private final NotificationTemplateService templateService = mock(NotificationTemplateService.class);
    private final NotificationOutboxWorker worker =
            new NotificationOutboxWorker(outboxService, templateService);

    @Test
    void sendsClaimedMessageThroughCurrentTemplateAndMarksItSent() {
        NotificationOutboxMessage message = message();
        when(outboxService.claimDueMessages()).thenReturn(List.of(message));

        worker.deliverDueMessages();

        verify(templateService).send(eq(NotificationTemplateType.ASIGNACION), any(NotificationEmailRequest.class));
        verify(outboxService).markSent(message);
    }

    @Test
    void retriesTemporarySmtpFailures() {
        NotificationOutboxMessage message = message();
        when(outboxService.claimDueMessages()).thenReturn(List.of(message));
        doThrow(new SmtpEmailException(SmtpEmailException.Reason.CONNECTION))
                .when(templateService).send(eq(NotificationTemplateType.ASIGNACION), any(NotificationEmailRequest.class));

        worker.deliverDueMessages();

        verify(outboxService).retry(message, "CONNECTION");
    }

    @Test
    void marksInvalidTemplateDataAsFailedWithoutRetrying() {
        NotificationOutboxMessage message = message();
        when(outboxService.claimDueMessages()).thenReturn(List.of(message));
        doThrow(new BusinessValidationException("Plantilla inactiva"))
                .when(templateService).send(eq(NotificationTemplateType.ASIGNACION), any(NotificationEmailRequest.class));

        worker.deliverDueMessages();

        verify(outboxService).fail(message, "Plantilla inactiva");
    }

    @Test
    void sendsTaskAssignmentCancellationThroughItsDedicatedTemplate() {
        NotificationOutboxMessage message = message("CANCELACION_ASIGNACION");
        when(outboxService.claimDueMessages()).thenReturn(List.of(message));

        worker.deliverDueMessages();

        verify(templateService).send(
                eq(NotificationTemplateType.CANCELACION_ASIGNACION),
                any(NotificationEmailRequest.class));
        verify(outboxService).markSent(message);
    }

    private NotificationOutboxMessage message() {
        return message("ASIGNACION");
    }

    private NotificationOutboxMessage message(String templateType) {
        return NotificationOutboxMessage.builder()
                .id(12L)
                .idempotencyKey("task-assignment:2:5")
                .templateType(templateType)
                .recipient("ana@example.com")
                .recipientName("Ana")
                .activity("Clasificar donaciones")
                .activityDate("25/04/2026 10:00")
                .status("PROCESSING")
                .attemptCount(1)
                .claimToken("claim-1")
                .build();
    }
}
