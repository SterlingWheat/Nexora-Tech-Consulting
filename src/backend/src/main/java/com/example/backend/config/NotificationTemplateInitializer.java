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
                            "Asignaci n",
                            "Nueva actividad asignada: {{actividad}}",
                            "<!doctype html><html lang=\"es\"><body style=\"font-family:Arial,sans-serif;line-height:1.6\">"
                                    + "<h1>Nueva asignaci n</h1><p>Hola {{nombre}},</p>"
                                    + "<p>Se te asign  la actividad <strong>{{actividad}}</strong>.</p>"
                                    + "<p>Fecha: {{fecha}}</p></body></html>",
                            "Nueva asignaci n\nHola {{nombre}},\nSe te asign  la actividad {{actividad}}.\nFecha: {{fecha}}"),
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
                            "Actualizaci n de turno: {{actividad}}",
                            "<!doctype html><html lang=\"es\"><body style=\"font-family:Arial,sans-serif;line-height:1.6\">"
                                    + "<h1>Cambio de turno</h1><p>Hola {{nombre}},</p>"
                                    + "<p>Hay una actualizaci n para el turno de <strong>{{actividad}}</strong>.</p>"
                                    + "<p>Nueva fecha: {{fecha}}</p></body></html>",
                            "Cambio de turno\nHola {{nombre}},\nHay una actualizaci n para el turno de {{actividad}}.\n"
                                    + "Nueva fecha: {{fecha}}"));

            if (repository.findByTemplateType("CANCELACION_ASIGNACION").isEmpty()) {
                repository.save(new NotificationTemplate(
                        "CANCELACION_ASIGNACION",
                        "Cancelaci n de asignaci n",
                        "Se cancel  tu asignaci n: {{actividad}}",
                        "<!doctype html><html lang=\"es\"><body style=\"font-family:Arial,sans-serif;line-height:1.6\">"
                                + "<h1>Cancelaci n de asignaci n</h1><p>Hola {{nombre}},</p>"
                                + "<p>Tu asignaci n para <strong>{{actividad}}</strong> fue cancelada.</p>"
                                + "<p>Fecha de cancelaci n: {{fecha}}</p></body></html>",
                        "Cancelaci n de asignaci n\nHola {{nombre}},\n"
                                + "Tu asignaci n para {{actividad}} fue cancelada.\nFecha de cancelaci n: {{fecha}}"));
            }

            for (NotificationTemplate template : defaults) {
                if (repository.findByTemplateType(template.getTemplateType()).isEmpty()) {
                    repository.save(template);
                }
            }
        };
    }
}