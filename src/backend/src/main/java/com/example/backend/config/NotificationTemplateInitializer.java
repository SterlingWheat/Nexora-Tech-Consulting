package com.example.backend.config;

import com.example.backend.model.NotificationTemplate;
import com.example.backend.repository.NotificationTemplateRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class NotificationTemplateInitializer {

    @Bean
    ApplicationRunner initializeNotificationTemplates(NotificationTemplateRepository repository) {
        return arguments -> {
            List<NotificationTemplate> defaults = List.of(
                    new NotificationTemplate(
                            "ASIGNACION",
                            "Asignación",
                            "Nueva actividad asignada: {{actividad}}",
                            "<!doctype html><html lang=\"es\"><body style=\"font-family:Arial,sans-serif;line-height:1.6\">"
                                    + "<h1>Nueva asignación</h1><p>Hola {{nombre}},</p>"
                                    + "<p>Se te asignó la actividad <strong>{{actividad}}</strong>.</p>"
                                    + "<p>Fecha: {{fecha}}</p></body></html>",
                            "Nueva asignación\nHola {{nombre}},\nSe te asignó la actividad {{actividad}}.\nFecha: {{fecha}}"),
                    new NotificationTemplate(
                            "RECORDATORIO",
                            "Recordatorio",
                            "Recordatorio de actividad: {{actividad}}",
                            "<!doctype html><html lang=\"es\"><body style=\"font-family:Arial,sans-serif;line-height:1.6\">"
                                    + "<h1>Recordatorio</h1><p>Hola {{nombre}},</p>"
                                    + "<p>Recuerda la actividad <strong>{{actividad}}</strong>.</p>"
                                    + "<p>Fecha: {{fecha}}</p></body></html>",
                            "Recordatorio\nHola {{nombre}},\nRecuerda la actividad {{actividad}}.\nFecha: {{fecha}}"),
                    new NotificationTemplate(
                            "CAMBIO_TURNO",
                            "Cambio de turno",
                            "Actualización de turno: {{actividad}}",
                            "<!doctype html><html lang=\"es\"><body style=\"font-family:Arial,sans-serif;line-height:1.6\">"
                                    + "<h1>Cambio de turno</h1><p>Hola {{nombre}},</p>"
                                    + "<p>Hay una actualización para el turno de <strong>{{actividad}}</strong>.</p>"
                                    + "<p>Nueva fecha: {{fecha}}</p></body></html>",
                            "Cambio de turno\nHola {{nombre}},\nHay una actualización para el turno de {{actividad}}.\n"
                                    + "Nueva fecha: {{fecha}"));
            for (NotificationTemplate template : defaults) {
                if (repository.findByTemplateType(template.getTemplateType()).isEmpty()) {
                    repository.save(template);
                }
            }
        };
    }
}
