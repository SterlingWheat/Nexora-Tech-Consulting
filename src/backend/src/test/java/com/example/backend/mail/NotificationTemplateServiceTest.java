package com.example.backend.mail;

import com.example.backend.dto.NotificationEmailRequest;
import com.example.backend.dto.NotificationTemplateUpdateRequest;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.model.NotificationTemplate;
import com.example.backend.repository.NotificationTemplateRepository;
import com.example.backend.service.SmtpEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationTemplateServiceTest {

    private NotificationTemplateRepository repository;
    private SmtpEmailService smtpEmailService;
    private NotificationTemplateService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationTemplateRepository.class);
        smtpEmailService = mock(SmtpEmailService.class);
        service = new NotificationTemplateService(repository, smtpEmailService);
    }

    @Test
    void enviaLasTresPlantillasConVariablesReemplazadasYHtmlEscapado() {
        for (NotificationTemplateType type : NotificationTemplateType.values()) {
            NotificationTemplate template = template(type);
            when(repository.findByTemplateType(type.name())).thenReturn(Optional.of(template));

            service.send(type, new NotificationEmailRequest(
                    "ana@example.com", "<Ana>", "Limpieza <salón>", "20/12/2026"));
        }
        verify(smtpEmailService, org.mockito.Mockito.times(NotificationTemplateType.values().length)).sendEmail(
                "ana@example.com",
                "Actividad: <Ana> / Limpieza <salón> / 20/12/2026",
                "<p>&lt;Ana&gt; - Limpieza &lt;sal&oacute;n&gt; - 20/12/2026</p>",
                "<Ana> - Limpieza <salón> - 20/12/2026");
    }

    @Test
    void guardaLaEdicionYElEnvioSiguienteUtilizaLaPlantillaPersistida() {
        NotificationTemplate template = template(NotificationTemplateType.ASIGNACION);
        when(repository.findByTemplateType("ASIGNACION")).thenReturn(Optional.of(template));
        when(repository.saveAndFlush(template)).thenReturn(template);
        NotificationTemplateUpdateRequest edited = new NotificationTemplateUpdateRequest(
                "Actualizada: {{actividad}}",
                "<main>{{nombre}} | {{actividad}} | {{fecha}}</main>",
                "{{nombre}} | {{actividad}} | {{fecha}}",
                true);

        service.update(NotificationTemplateType.ASIGNACION, edited);
        service.send(NotificationTemplateType.ASIGNACION,
                new NotificationEmailRequest("ana@example.com", "Ana", "Nueva tarea", "21/12/2026"));

        verify(repository).saveAndFlush(template);
        verify(smtpEmailService).sendEmail(
                "ana@example.com",
                "Actualizada: Nueva tarea",
                "<main>Ana | Nueva tarea | 21/12/2026</main>",
                "Ana | Nueva tarea | 21/12/2026");
        assertEquals(edited.subject(), template.getSubject());
    }

    @Test
    void rechazaVariablesNoSoportadasYPlantillasInactivas() {
        NotificationTemplate template = template(NotificationTemplateType.RECORDATORIO);
        when(repository.findByTemplateType("RECORDATORIO")).thenReturn(Optional.of(template));

        assertThrows(BusinessValidationException.class, () -> service.update(
                NotificationTemplateType.RECORDATORIO,
                new NotificationTemplateUpdateRequest(
                        "{{nombre}}",
                        "<p>{{nombre}} {{actividad}} {{fecha}} {{secreto}}</p>",
                        "{{nombre}} {{actividad}} {{fecha}}",
                        true)));
        verify(repository, never()).saveAndFlush(any());

        template.update(template.getSubject(), template.getHtmlBody(), template.getTextBody(), false);
        assertThrows(BusinessValidationException.class, () -> service.send(
                NotificationTemplateType.RECORDATORIO,
                new NotificationEmailRequest("ana@example.com", "Ana", "Actividad", "20/12/2026")));
        verify(smtpEmailService, never()).sendEmail(any(), any(), any(), any());
    }

    @Test
    void noEnviaPlantillasConDatosRequeridosAusentes() {
        when(repository.findByTemplateType("ASIGNACION"))
                .thenReturn(Optional.of(template(NotificationTemplateType.ASIGNACION)));

        assertThrows(BusinessValidationException.class, () -> service.send(
                NotificationTemplateType.ASIGNACION,
                new NotificationEmailRequest("ana@example.com", "Ana", " ", "20/12/2026")));
        verify(smtpEmailService, never()).sendEmail(any(), any(), any(), any());
    }

    @Test
    void noEnviaContenidoGuardadoConVariableIncompleta() {
        NotificationTemplate template = new NotificationTemplate(
                "ASIGNACION",
                "Asignación",
                "{{nombre}} {{actividad}} {{fecha}}",
                "<p>{{nombre {{actividad}} {{fecha}}</p>",
                "{{nombre}} {{actividad}} {{fecha}}");
        when(repository.findByTemplateType("ASIGNACION")).thenReturn(Optional.of(template));

        assertThrows(BusinessValidationException.class, () -> service.send(
                NotificationTemplateType.ASIGNACION,
                new NotificationEmailRequest("ana@example.com", "Ana", "Actividad", "20/12/2026")));
        verify(smtpEmailService, never()).sendEmail(any(), any(), any(), any());
    }

    private NotificationTemplate template(NotificationTemplateType type) {
        return new NotificationTemplate(
                type.name(),
                type.displayName(),
                "Actividad: {{nombre}} / {{actividad}} / {{fecha}}",
                "<p>{{nombre}} - {{actividad}} - {{fecha}}</p>",
                "{{nombre}} - {{actividad}} - {{fecha}}");
    }
}
