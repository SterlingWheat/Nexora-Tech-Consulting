package com.example.backend.config;

import com.example.backend.model.NotificationTemplate;
import com.example.backend.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationTemplateInitializerTest {

    @Test
    void creaSoloPlantillasAusentesYConservaLasVersionesGuardadas() throws Exception {
        NotificationTemplateRepository repository = mock(NotificationTemplateRepository.class);
        NotificationTemplate edited = new NotificationTemplate(
                "ASIGNACION", "Asignación", "Asunto editado", "<p>HTML</p>", "Texto editado");
        when(repository.findByTemplateType("ASIGNACION")).thenReturn(Optional.of(edited));
        when(repository.findByTemplateType("RECORDATORIO")).thenReturn(Optional.empty());
        when(repository.findByTemplateType("CAMBIO_TURNO")).thenReturn(Optional.empty());
        when(repository.findByTemplateType("CANCELACION_ASIGNACION")).thenReturn(Optional.empty());

        new NotificationTemplateInitializer()
                .initializeNotificationTemplates(repository)
                .run(new DefaultApplicationArguments(new String[0]));

        verify(repository, never()).save(edited);
        verify(repository, org.mockito.Mockito.times(3)).save(any(NotificationTemplate.class));
    }
}
