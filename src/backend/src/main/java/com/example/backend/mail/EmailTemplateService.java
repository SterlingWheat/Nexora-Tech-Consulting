package com.example.backend.mail;

import com.example.backend.service.SmtpEmailService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class EmailTemplateService {

    private final EmailTemplateRenderer renderer;
    private final SmtpEmailService smtpEmailService;

    public EmailTemplateService(EmailTemplateRenderer renderer, SmtpEmailService smtpEmailService) {
        this.renderer = renderer;
        this.smtpEmailService = smtpEmailService;
    }

    public RenderedEmail render(EmailTemplateType type, EmailTemplateData data) {
        return renderer.render(type, data);
    }

    public void sendWelcome(String recipient, String name) {
        send(recipient, EmailTemplateType.WELCOME,
                new EmailTemplateData(name, recipient, null, null, null, null));
    }

    public void sendPasswordRecovery(
            String recipient,
            String name,
            String actionUrl,
            LocalDateTime expiresAt) {
        send(recipient, EmailTemplateType.PASSWORD_RECOVERY,
                new EmailTemplateData(name, recipient, null, actionUrl, "Restablecer contraseña", expiresAt));
    }

    public void sendPasswordReset(String recipient, String name) {
        send(recipient, EmailTemplateType.PASSWORD_RESET,
                new EmailTemplateData(name, recipient, null, null, null, null));
    }

    public void sendAdminNotification(String recipient, String name, String details) {
        send(recipient, EmailTemplateType.ADMIN_NOTIFICATION,
                new EmailTemplateData(name, recipient, details, null, null, null));
    }

    public void sendAccountChange(String recipient, String name, String details) {
        send(recipient, EmailTemplateType.ACCOUNT_CHANGE,
                new EmailTemplateData(name, recipient, details, null, null, null));
    }

    public void sendOperationConfirmation(String recipient, String name, String details) {
        send(recipient, EmailTemplateType.OPERATION_CONFIRMATION,
                new EmailTemplateData(name, recipient, details, null, null, null));
    }

    private void send(String recipient, EmailTemplateType type, EmailTemplateData data) {
        Objects.requireNonNull(recipient, "El destinatario es obligatorio.");
        RenderedEmail email = renderer.render(type, data);
        smtpEmailService.sendEmail(recipient, email.subject(), email.html(), email.text());
    }
}
