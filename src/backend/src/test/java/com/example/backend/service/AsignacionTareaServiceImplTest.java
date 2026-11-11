package com.example.backend.service;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ConflictException;
import com.example.backend.model.AsignacionTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Voluntario;
import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.notification.NotificationOutboxService;
import com.example.backend.repository.AsignacionTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.impl.AsignacionTareaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class AsignacionTareaServiceImplTest {

    @Mock
    private AsignacionTareaRepository asignacionTareaRepository;

    @Mock
    private TareaRepository tareaRepository;

    @Mock
    private VoluntarioRepository voluntarioRepository;

    @Mock
    private HistorialTareaService historialTareaService;

    @Mock
    private NotificationOutboxService notificationOutboxService;

    @InjectMocks
    private AsignacionTareaServiceImpl asignacionService;

    @Test
    void asignarVoluntarios_CambiaEstadoAAsignadaYRegistraHistorial() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(10);
        tarea.setEstado("Pendiente");
        tarea.setTitulo("Limpieza de sala");

        Voluntario voluntario = Voluntario.builder()
                .idVoluntario(5)
                .activo(true)
                .nombre("Ana")
                .apellido("Quispe")
                .email("ana@example.com")
                .build();

        when(tareaRepository.findById(10)).thenReturn(Optional.of(tarea));
        when(voluntarioRepository.findById(5)).thenReturn(Optional.of(voluntario));
        when(asignacionTareaRepository.existsActiveByTareaAndVoluntario(10, 5)).thenReturn(false);
        when(asignacionTareaRepository.saveAll(any())).thenAnswer(invocation -> {
            List<AsignacionTarea> assignments = invocation.getArgument(0);
            assignments.get(0).setIdAsignacion(101);
            return assignments;
        });

        List<AsignacionTarea> resultado = asignacionService.asignarVoluntarios(10, List.of(5), 1);

        assertEquals(1, resultado.size());
        assertEquals("Asignada", tarea.getEstado());
        verify(tareaRepository).save(tarea);
        verify(historialTareaService).registrarAccion(eq(10), eq(1), contains("Ana Quispe"));
        verify(notificationOutboxService).enqueue(
                eq("task-assignment:101"),
                eq(NotificationTemplateType.ASIGNACION),
                eq("ana@example.com"),
                eq("Ana Quispe"),
                eq("Limpieza de sala"),
                argThat(date -> date != null && !date.isBlank()));
    }

    @Test
    void asignarVoluntarios_LanzaExcepcionSiVoluntarioEstaInactivo() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(10);
        tarea.setEstado("Pendiente");

        Voluntario voluntarioInactivo = Voluntario.builder()
                .idVoluntario(5)
                .activo(false)
                .nombre("Carlos")
                .apellido("Perez")
                .build();

        when(tareaRepository.findById(10)).thenReturn(Optional.of(tarea));
        when(voluntarioRepository.findById(5)).thenReturn(Optional.of(voluntarioInactivo));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> {
            asignacionService.asignarVoluntarios(10, List.of(5), 1);
        });

        // Verificamos palabras sin tilde para evitar errores de codificación
        assertTrue(ex.getMessage().contains("activo") && ex.getMessage().contains("no puede ser asignado"));
        verify(asignacionTareaRepository, never()).saveAll(any());
    }

    @Test
    void cancelarAsignacionConservaRegistroNotificaYDevuelveTareaSinAsignadosAPendiente() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(10);
        tarea.setEstado("Asignada");
        tarea.setTitulo("Limpieza de sala");
        Voluntario voluntario = Voluntario.builder()
                .idVoluntario(5)
                .nombre("Ana")
                .apellido("Quispe")
                .email("ana@example.com")
                .build();
        AsignacionTarea asignacion = AsignacionTarea.builder()
                .idAsignacion(101)
                .tarea(tarea)
                .voluntario(voluntario)
                .estadoRespuesta("Asignado")
                .build();

        when(tareaRepository.findById(10)).thenReturn(Optional.of(tarea));
        when(asignacionTareaRepository.findById(101)).thenReturn(Optional.of(asignacion));
        when(asignacionTareaRepository.countActiveByTareaId(10)).thenReturn(0L);

        asignacionService.cancelarAsignacion(10, 101, 2);

        assertEquals("Cancelada", asignacion.getEstadoRespuesta());
        assertEquals("Pendiente", tarea.getEstado());
        verify(asignacionTareaRepository).save(asignacion);
        verify(asignacionTareaRepository, never()).delete(any());
        verify(notificationOutboxService).enqueue(
                eq("task-assignment-cancellation:101"),
                eq(NotificationTemplateType.CANCELACION_ASIGNACION),
                eq("ana@example.com"),
                eq("Ana Quispe"),
                eq("Limpieza de sala"),
                argThat(date -> date != null && !date.isBlank()));
        verify(historialTareaService).registrarAccion(
                eq(10), eq(2), contains("Ana Quispe"));
    }

    @Test
    void cancelarAsignacionRechazaUnRegistroYaCancelado() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(10);
        Voluntario voluntario = Voluntario.builder()
                .idVoluntario(5)
                .nombre("Ana")
                .apellido("Quispe")
                .email("ana@example.com")
                .build();
        AsignacionTarea asignacion = AsignacionTarea.builder()
                .idAsignacion(101)
                .tarea(tarea)
                .voluntario(voluntario)
                .estadoRespuesta("Cancelada")
                .build();
        when(tareaRepository.findById(10)).thenReturn(Optional.of(tarea));
        when(asignacionTareaRepository.findById(101)).thenReturn(Optional.of(asignacion));

        assertThrows(ConflictException.class, () -> asignacionService.cancelarAsignacion(10, 101, 2));
        verify(asignacionTareaRepository, never()).save(any());
        verifyNoInteractions(notificationOutboxService);
    }

    @Test
    void asignarVoluntarios_LanzaExcepcionSiTareaEstaFinalizada() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(10);
        tarea.setEstado("Finalizada");

        when(tareaRepository.findById(10)).thenReturn(Optional.of(tarea));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> {
            asignacionService.asignarVoluntarios(10, List.of(5), 1);
        });

        assertTrue(ex.getMessage().contains("No se pueden asignar voluntarios a una tarea Finalizada o Cancelada"));
        verify(voluntarioRepository, never()).findById(any());
    }
}