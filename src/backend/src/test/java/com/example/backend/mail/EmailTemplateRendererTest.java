package com.example.backend.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplateRendererTest {

    private EmailTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneId.of("America/La_Paz"));
        renderer = new EmailTemplateRenderer(clock, "Nexora Tech Consulting");
    }

    @Test
    void renderizaTodasLasPlantillasConHtmlYTexto() {
        EmailTemplateData data = new EmailTemplateData(
                "Ana", "ana@example.com", "La operación se completó.", null, null, null);

        for (EmailTemplateType type : EmailTemplateType.values()) {
            if (type == EmailTemplateType.PASSWORD_RECOVERY) {
                continue;
            }
            RenderedEmail rendered = renderer.render(type, data);
            assertTrue(rendered.subject().contains("Nexora Tech Consulting"));
            assertTrue(rendered.html().contains("<html lang=\"es\">"));
            assertTrue(rendered.text().contains("Ana"));
            assertFalse(rendered.html().contains("null"));
            assertFalse(rendered.text().contains("null"));
        }
    }

    @Test
    void escapaDatosDinamicosEnHtmlYConservaTextoPlanoLegible() {
        EmailTemplateData data = new EmailTemplateData(
                "<Ana>", "ana@example.com", "<script>alert('x')</script>", null, null, null);

        RenderedEmail rendered = renderer.render(EmailTemplateType.ACCOUNT_CHANGE, data);

        assertTrue(rendered.html().contains("&lt;Ana&gt;"));
        assertTrue(rendered.html().contains("&lt;script&gt;"));
        assertFalse(rendered.html().contains("<script>"));
        assertTrue(rendered.text().contains("<script>alert('x')</script>"));
    }

    @Test
    void nombreAusenteUsaSaludoSeguroEnTextoPlano() {
        RenderedEmail rendered = renderer.render(
                EmailTemplateType.WELCOME,
                new EmailTemplateData(null, "ana@example.com", null, null, null, null));

        assertTrue(rendered.text().startsWith("Nexora Tech Consulting\nTe damos la bienvenida\n\nHola equipo,"));
        assertFalse(rendered.text().contains("null"));
    }

    @Test
    void recuperacionExigeEnlaceSeguroYFechaFutura() {
        EmailTemplateData validData = new EmailTemplateData(
                "Ana",
                "ana@example.com",
                null,
                "https://nexora.example/reset?token=abc",
                "Restablecer contraseña",
                LocalDateTime.of(2026, 1, 15, 10, 0));
        RenderedEmail rendered = renderer.render(EmailTemplateType.PASSWORD_RECOVERY, validData);
        assertTrue(rendered.html().contains("https://nexora.example/reset?token=abc"));

        EmailTemplateData unsafeUrl = new EmailTemplateData(
                "Ana", "ana@example.com", null, "javascript:alert(1)", "Abrir", validData.expiresAt());
        assertThrows(IllegalArgumentException.class,
                () -> renderer.render(EmailTemplateType.PASSWORD_RECOVERY, unsafeUrl));

        EmailTemplateData expired = new EmailTemplateData(
                "Ana", "ana@example.com", null, validData.actionUrl(), "Abrir",
                LocalDateTime.of(2026, 1, 15, 7, 0));
        assertThrows(IllegalArgumentException.class,
                () -> renderer.render(EmailTemplateType.PASSWORD_RECOVERY, expired));
    }
}
