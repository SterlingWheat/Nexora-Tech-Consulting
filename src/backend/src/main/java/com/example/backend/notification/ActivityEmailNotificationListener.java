package com.example.backend.notification;

import com.example.backend.dto.NotificationEmailRequest;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.SmtpEmailException;
import com.example.backend.mail.NotificationTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ActivityEmailNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(ActivityEmailNotificationListener.class);

    private final NotificationTemplateService templateService;

    public ActivityEmailNotificationListener(NotificationTemplateService templateService) {
        this.templateService = templateService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void send(ActivityEmailNotificationEvent event) {
        try {
            templateService.send(
                    event.templateType(),
                    new NotificationEmailRequest(event.recipient(), event.name(), event.activity(), event.date()));
        } catch (SmtpEmailException exception) {
            logger.warn("Activity email delivery failed: type={}, reason={}",
                    event.templateType(), exception.getReason());
        } catch (IllegalArgumentException exception) {
            logger.warn("Activity email notification was rejected: type={}", event.templateType());
        } catch (BusinessValidationException exception) {
            logger.warn("Activity email notification could not be sent: type={}, reason={}",
                    event.templateType(), exception.getMessage());
        }
    }
}
