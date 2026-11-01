package com.example.backend.notification;

import com.example.backend.exception.SmtpEmailException;
import com.example.backend.mail.EmailTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AccountEmailNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(AccountEmailNotificationListener.class);

    private final EmailTemplateService emailTemplateService;

    public AccountEmailNotificationListener(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendAccountNotification(AccountEmailNotificationEvent event) {
        try {
            switch (event.kind()) {
                case WELCOME -> emailTemplateService.sendWelcome(event.recipient(), event.recipientName());
                case ACCOUNT_CHANGE -> emailTemplateService.sendAccountChange(
                        event.recipient(), event.recipientName(), event.details());
                case PASSWORD_RESET -> emailTemplateService.sendPasswordReset(
                        event.recipient(), event.recipientName());
            }
        } catch (SmtpEmailException exception) {
            logger.warn("Account email notification failed: kind={}, reason={}",
                    event.kind(), exception.getReason());
        } catch (IllegalArgumentException exception) {
            logger.warn("Account email notification rejected because its input was invalid: kind={}",
                    event.kind());
        }
    }
}
