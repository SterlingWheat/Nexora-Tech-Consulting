package com.example.backend.service;

import com.example.backend.exception.SmtpEmailException;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PasswordResetEmailSenderTest {

    @Test
    void sendsAResetEmailWithBrandExpiryWarningAndFrontendLink() {
        SmtpEmailService smtpEmailService = mock(SmtpEmailService.class);
        PasswordResetEmailSender sender = new PasswordResetEmailSender(smtpEmailService);

        sender.enviar("persona@example.com", "token_seguro-123", 30, "https://app.example.com/");

        verify(smtpEmailService).sendEmail(
                eq("persona@example.com"),
                argThat(subject -> subject.contains("Restablece tu contrase") && subject.contains("Nexora Tech Consulting")),
                contains("https://app.example.com/restablecer-contrasena#token=token_seguro-123"),
                argThat(body -> body.contains("vence en 30 minutos") && body.contains("ignora este correo")));
    }

    @Test
    void logsSmtpFailureWithoutPropagatingSecretsToThePublicRequest() {
        SmtpEmailService smtpEmailService = mock(SmtpEmailService.class);
        doThrow(new SmtpEmailException(SmtpEmailException.Reason.AUTHENTICATION))
                .when(smtpEmailService).sendEmail(
                        eq("persona@example.com"),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
                        
        PasswordResetEmailSender sender = new PasswordResetEmailSender(smtpEmailService);

        sender.enviar("persona@example.com", "token_seguro-123", 30, "https://app.example.com");
    }
}