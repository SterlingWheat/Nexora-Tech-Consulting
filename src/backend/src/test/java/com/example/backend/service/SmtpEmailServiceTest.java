package com.example.backend.service;

import com.example.backend.config.SmtpProperties;
import com.example.backend.exception.SmtpEmailException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.SocketTimeoutException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class SmtpEmailServiceTest {

    private static final String SMTP_PASSWORD = "smtp-secret-for-test-only";

    private JavaMailSender mailSender;
    private SmtpProperties smtpProperties;
    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        smtpProperties = configuredProperties();
        emailService = new SmtpEmailService(mailSender, smtpProperties);
    }

    @Test
    void sendsPlainTextAndHtmlThroughConfiguredTransport() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendEmail(
                "destinatario@example.com",
                "Asunto de prueba",
                "<p>Contenido HTML</p>",
                "Contenido de texto");

        verify(mailSender).send(message);
        assertEquals("Asunto de prueba", message.getSubject());
        assertEquals("destinatario@example.com", message.getAllRecipients()[0].toString());
    }

    @Test
    void refusesToSendWhenRequiredConfigurationIsMissing() {
        smtpProperties.setPassword("");

        SmtpEmailException exception = assertThrows(
                SmtpEmailException.class,
                () -> emailService.sendEmail("destinatario@example.com", "Asunto", null, "Texto"));

        assertEquals(SmtpEmailException.Reason.CONFIGURATION, exception.getReason());
        assertTrue(!exception.getMessage().contains(SMTP_PASSWORD));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void rejectsInvalidRecipientBeforeSending() {
        assertThrows(
                IllegalArgumentException.class,
                () -> emailService.sendEmail("no-es-un-correo", "Asunto", null, "Texto"));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void reportsAuthenticationFailureWithoutExposingProviderDetails() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailAuthenticationException("provider echoed " + SMTP_PASSWORD))
                .when(mailSender).send(message);

        SmtpEmailException exception = assertThrows(
                SmtpEmailException.class,
                () -> emailService.sendEmail("destinatario@example.com", "Asunto", null, "Texto"));

        assertEquals(SmtpEmailException.Reason.AUTHENTICATION, exception.getReason());
        assertTrue(!exception.getMessage().contains(SMTP_PASSWORD));
    }

    @Test
    void reportsTimeoutWithoutExposingProviderDetails() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("provider echoed " + SMTP_PASSWORD, new SocketTimeoutException()))
                .when(mailSender).send(message);

        SmtpEmailException exception = assertThrows(
                SmtpEmailException.class,
                () -> emailService.sendEmail("destinatario@example.com", "Asunto", null, "Texto"));

        assertEquals(SmtpEmailException.Reason.TIMEOUT, exception.getReason());
        assertTrue(!exception.getMessage().contains(SMTP_PASSWORD));
    }

    private SmtpProperties configuredProperties() {
        SmtpProperties properties = new SmtpProperties();
        properties.setHost("smtp.gmail.com");
        properties.setPort(465);
        properties.setSecure(true);
        properties.setUsername("sender@example.com");
        properties.setPassword(SMTP_PASSWORD);
        properties.setFrom("sender@example.com");
        return properties;
    }
}
