package com.example.backend.service;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.model.AsignacionTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.AsignacionTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.impl.AsignacionTareaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private AsignacionTareaServiceImpl asignacionService;

    @Test
    void asignarVoluntarios_CambiaEstadoAAsignadaYRegistraHistorial() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(10);
        tarea.setEstado("Pendiente");

        Voluntario voluntario = Voluntario.builder()
                .idVoluntario(5)
                .activo(true)
                .nombre("Ana")
                .apellido("Quispe")
                .build();

        when(tareaRepository.findById(10)).thenReturn(Optional.of(tarea));
        when(voluntarioRepository.findById(5)).thenReturn(Optional.of(voluntario));
        when(asignacionTareaRepository.existsByTareaIdTareaAndVoluntarioIdVoluntario(10, 5)).thenReturn(false);
        when(asignacionTareaRepository.saveAll(any())).thenReturn(Collections.emptyList());

        List<AsignacionTarea> resultado = asignacionService.asignarVoluntarios(10, List.of(5), 1);

        assertEquals(1, resultado.size());
        assertEquals("Asignada", tarea.getEstado());
        verify(tareaRepository).save(tarea);
        verify(historialTareaService).registrarAccion(eq(10), eq(1), contains("Ana Quispe"));
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

        assertTrue(ex.getMessage().contains("no está activo"));
        verify(asignacionTareaRepository, never()).saveAll(any());
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