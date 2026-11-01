package com.example.backend.mail;

import com.example.backend.service.SmtpEmailService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailTemplateServiceTest {

    @Test
    void renderizaYEnviaUsandoElServicioSmtpExistente() {
        EmailTemplateRenderer renderer = mock(EmailTemplateRenderer.class);
        SmtpEmailService smtpEmailService = mock(SmtpEmailService.class);
        EmailTemplateService service = new EmailTemplateService(renderer, smtpEmailService);
        RenderedEmail rendered = new RenderedEmail("Bienvenido", "<p>HTML</p>", "Texto");
        when(renderer.render(
                EmailTemplateType.WELCOME,
                new EmailTemplateData("Ana", "ana@example.com", null, null, null, null)))
                .thenReturn(rendered);

        service.sendWelcome("ana@example.com", "Ana");

        verify(renderer).render(
                EmailTemplateType.WELCOME,
                new EmailTemplateData("Ana", "ana@example.com", null, null, null, null));
        verify(smtpEmailService).sendEmail("ana@example.com", "Bienvenido", "<p>HTML</p>", "Texto");
    }
}
