package com.example.backend.controller;

import com.example.backend.exception.SmtpEmailException;
import com.example.backend.service.SmtpEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class EmailTestControllerTest {

    private SmtpEmailService emailService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        emailService = mock(SmtpEmailService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new EmailTestController(emailService))
                .setValidator(validator)
                .build();
    }

    @Test
    void sendsTestMessageAndReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/admin/email/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"destinatario@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"Correo de prueba enviado correctamente.\"}"));

        verify(emailService).sendEmail(
                "destinatario@example.com",
                "Prueba de correo - Nexora",
                "<p>Este es un correo de prueba enviado por Nexora Tech Consulting.</p>",
                "Este es un correo de prueba enviado por Nexora Tech Consulting.");
    }

    @Test
    void rejectsInvalidRecipientWithoutCallingEmailService() throws Exception {
        mockMvc.perform(post("/api/admin/email/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"correo-invalido"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(emailService);
    }

    @Test
    void respondsSafelyWhenSmtpConfigurationIsMissing() throws Exception {
        doThrow(new SmtpEmailException(SmtpEmailException.Reason.CONFIGURATION))
                .when(emailService).sendEmail(
                        "destinatario@example.com",
                        "Prueba de correo - Nexora",
                        "<p>Este es un correo de prueba enviado por Nexora Tech Consulting.</p>",
                        "Este es un correo de prueba enviado por Nexora Tech Consulting.");

        mockMvc.perform(post("/api/admin/email/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"destinatario@example.com"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"message\":\"El servicio de correo no está configurado.\"}"));
    }

    @Test
    void respondsSafelyToSmtpDeliveryFailure() throws Exception {
        doThrow(new SmtpEmailException(SmtpEmailException.Reason.AUTHENTICATION))
                .when(emailService).sendEmail(
                        "destinatario@example.com",
                        "Prueba de correo - Nexora",
                        "<p>Este es un correo de prueba enviado por Nexora Tech Consulting.</p>",
                        "Este es un correo de prueba enviado por Nexora Tech Consulting.");

        mockMvc.perform(post("/api/admin/email/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"destinatario@example.com"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(content().json("{\"message\":\"No se pudo enviar el correo. Verifica la configuración SMTP.\"}"));
    }
}
