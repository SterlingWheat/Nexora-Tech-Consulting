package com.example.backend.service;

import com.example.backend.config.SmtpProperties;
import com.example.backend.exception.SmtpEmailException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

@Service
public class SmtpEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final SmtpProperties smtpProperties;

    public SmtpEmailService(JavaMailSender mailSender, SmtpProperties smtpProperties) {
        this.mailSender = mailSender;
        this.smtpProperties = smtpProperties;
    }

    public void sendEmail(String recipient, String subject, String html, String text) {
        if (!smtpProperties.isConfigured()) {
            logger.warn("SMTP delivery unavailable: configuration is incomplete");
            throw new SmtpEmailException(SmtpEmailException.Reason.CONFIGURATION);
        }

        validateAddress(recipient);
        if (subject == null || subject.isBlank() || (isBlank(html) && isBlank(text))) {
            throw new IllegalArgumentException("El asunto y al menos un cuerpo del correo son obligatorios.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(smtpProperties.getFrom());
            helper.setTo(recipient);
            helper.setSubject(subject);
            if (!isBlank(html) && !isBlank(text)) {
                helper.setText(text, html);
            } else if (!isBlank(html)) {
                helper.setText(html, true);
            } else {
                helper.setText(text, false);
            }
            mailSender.send(message);
        } catch (MailException exception) {
            SmtpEmailException.Reason reason = classify(exception);
            logger.warn("SMTP delivery failed: {}", reason);
            throw new SmtpEmailException(reason);
        } catch (MessagingException exception) {
            logger.warn("SMTP message could not be prepared");
            throw new SmtpEmailException(SmtpEmailException.Reason.DELIVERY);
        }
    }

    private void validateAddress(String recipient) {
        try {
            InternetAddress address = new InternetAddress(recipient, true);
            address.validate();
        } catch (AddressException | NullPointerException exception) {
            throw new IllegalArgumentException("El destinatario debe ser un correo válido.");
        }
    }

    private SmtpEmailException.Reason classify(MailException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof MailAuthenticationException
                    || cause instanceof AuthenticationFailedException) {
                return SmtpEmailException.Reason.AUTHENTICATION;
            }
            if (cause instanceof SocketTimeoutException) {
                return SmtpEmailException.Reason.TIMEOUT;
            }
            if (cause instanceof ConnectException) {
                return SmtpEmailException.Reason.CONNECTION;
            }
            cause = cause.getCause();
        }
        return SmtpEmailException.Reason.DELIVERY;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
