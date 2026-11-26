package com.example.backend.notification;

import com.example.backend.dto.NotificationEmailRequest;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.SmtpEmailException;
import com.example.backend.mail.NotificationTemplateService;
import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.NotificationOutboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationOutboxWorker {

    private static final Logger logger = LoggerFactory.getLogger(NotificationOutboxWorker.class);

    private final NotificationOutboxService outboxService;
    private final NotificationTemplateService templateService;

    public NotificationOutboxWorker(
            NotificationOutboxService outboxService,
            NotificationTemplateService templateService) {
        this.outboxService = outboxService;
        this.templateService = templateService;
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-interval-ms:5000}")
    public void deliverDueMessages() {
        for (NotificationOutboxMessage message : outboxService.claimDueMessages()) {
            deliver(message);
        }
    }

    private void deliver(NotificationOutboxMessage message) {
        try {
            templateService.send(
                    NotificationTemplateType.fromPath(message.getTemplateType()),
                    new NotificationEmailRequest(
                            message.getRecipient(),
                            message.getRecipientName(),
                            message.getActivity(),
                            message.getActivityDate()));
            outboxService.markSent(message);
            
            // HU1 CA1: Log de trazabilidad exitosa para el equipo de TI
            logger.info("Envío de correo exitoso: id={}, destinatario={}, plantilla={}",
                    message.getId(), message.getRecipient(), message.getTemplateType());
            
        } catch (SmtpEmailException exception) {
            // HU1 CA2: Log de reintento por fallo de red o SMTP
            logger.warn("Fallo temporal enviando correo. Programando reintento: id={}, destinatario={}, intentoActual={}, error={}",
                    message.getId(), message.getRecipient(), message.getAttemptCount(), exception.getReason());
            outboxService.retry(message, exception.getReason().name());
            
        } catch (BusinessValidationException | ResourceNotFoundException | IllegalArgumentException exception) {
            // HU1 CA1: Log de fallo definitivo (plantilla no existe o datos inválidos)
            logger.error("Fallo definitivo enviando correo. No se reintentará: id={}, destinatario={}, error={}",
                    message.getId(), message.getRecipient(), exception.getMessage());
            outboxService.fail(message, exception.getMessage());
        }
    }
}