package com.example.backend.service;

import com.example.backend.exception.SmtpEmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailSender {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailSender.class);

    private final SmtpEmailService smtpEmailService;

    public PasswordResetEmailSender(SmtpEmailService smtpEmailService) {
        this.smtpEmailService = smtpEmailService;
    }

    @Async("passwordResetTaskExecutor")
    public void enviar(String recipient, String rawToken, long expiryMinutes, String frontendUrl) {
        String resetUrl = frontendUrl.replaceAll("/+$", "")
                + "/restablecer-contrasena#token=" + rawToken;
        String subject = "Restablece tu contraseña - Nexora Tech Consulting";
        String text = """
                Recibimos una solicitud para restablecer la contraseña de tu cuenta de Nexora Tech Consulting.

                Abre este enlace para crear una nueva contraseña:
                %s

                El enlace vence en %d minutos. Si no solicitaste este cambio, ignora este correo.
                """.formatted(resetUrl, expiryMinutes);
        String html = """
                <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta de <strong>Nexora Tech Consulting</strong>.</p>
                <p><a href="%s">Restablecer contraseña</a></p>
                <p>El enlace vence en %d minutos. Si no solicitaste este cambio, ignora este correo.</p>
                """.formatted(resetUrl, expiryMinutes);

        try {
            smtpEmailService.sendEmail(recipient, subject, html, text);
        } catch (SmtpEmailException exception) {
            logger.warn("No se pudo entregar el correo de recuperación; revisa la configuración SMTP.");
        }
    }
}
