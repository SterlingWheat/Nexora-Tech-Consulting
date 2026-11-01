package com.example.backend.notification;

import com.example.backend.exception.SmtpEmailException;
import com.example.backend.mail.EmailTemplateService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class AccountEmailNotificationListenerTest {

    @Test
    void falloSmtpNoRevierteLaOperacionDeCuenta() {
        EmailTemplateService emailTemplateService = mock(EmailTemplateService.class);
        AccountEmailNotificationListener listener = new AccountEmailNotificationListener(emailTemplateService);
        doThrow(new SmtpEmailException(SmtpEmailException.Reason.AUTHENTICATION))
                .when(emailTemplateService).sendWelcome("ana@example.com", "Ana");

        assertDoesNotThrow(() -> listener.sendAccountNotification(new AccountEmailNotificationEvent(
                AccountEmailNotificationEvent.Kind.WELCOME,
                "ana@example.com",
                "Ana",
                "Tu cuenta fue creada.")));
    }

    @Test
    void destinatarioInvalidoSeRegistraSinInterrumpirLaOperacionDeCuenta() {
        EmailTemplateService emailTemplateService = mock(EmailTemplateService.class);
        AccountEmailNotificationListener listener = new AccountEmailNotificationListener(emailTemplateService);
        doThrow(new IllegalArgumentException("Invalid email"))
                .when(emailTemplateService).sendWelcome("correo-invalido", "Ana");

        assertDoesNotThrow(() -> listener.sendAccountNotification(new AccountEmailNotificationEvent(
                AccountEmailNotificationEvent.Kind.WELCOME,
                "correo-invalido",
                "Ana",
                "Tu cuenta fue creada.")));
    }
}
