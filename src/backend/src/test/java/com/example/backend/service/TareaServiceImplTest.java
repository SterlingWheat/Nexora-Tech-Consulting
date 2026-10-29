package com.example.backend.service;

import com.example.backend.dto.RegistroAvanceRequestDTO;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import com.example.backend.repository.CategoriaTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.service.impl.TareaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TareaServiceImplTest {

    @Mock
    private TareaRepository tareaRepository;

    @Mock
    private CategoriaTareaRepository categoriaTareaRepository;

    @Mock
    private HistorialTareaService historialTareaService;

    @InjectMocks
    private TareaServiceImpl tareaService;

    private CategoriaTarea categoriaMock;

    @BeforeEach
    void setUp() {
        categoriaMock = CategoriaTarea.builder()
                .idCategoria(1)
                .nombre("Logística")
                .build();
    }

    @Test
    void crearTarea_DebeInicializarEnEstadoPendienteYAvanceCero() {
        Tarea tarea = new Tarea();
        tarea.setTitulo("Organizar donaciones");
        tarea.setCategoria(categoriaMock);

        when(categoriaTareaRepository.findById(1)).thenReturn(Optional.of(categoriaMock));
        when(tareaRepository.save(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tarea resultado = tareaService.crearTarea(tarea);

        assertNotNull(resultado);
        assertEquals("Pendiente", resultado.getEstado());
        assertEquals((short) 0, resultado.getAvance());
        verify(tareaRepository).save(any(Tarea.class));
    }

    @Test
    void cambiarEstado_LanzaExcepcionSiTareaEstaEnEstadoTerminal() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(1);
        tarea.setEstado("Finalizada");

        when(tareaRepository.findById(1)).thenReturn(Optional.of(tarea));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> {
            tareaService.cambiarEstado(1, "En progreso", 99);
        });

        assertTrue(ex.getMessage().contains("Transición no permitida"));
        verify(historialTareaService, never()).registrarAccion(any(), any(), any());
    }

    @Test
    void cambiarEstado_ActualizaEstadoYRegistraHistorial() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(2);
        tarea.setEstado("Pendiente");

        when(tareaRepository.findById(2)).thenReturn(Optional.of(tarea));
        when(tareaRepository.save(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tarea resultado = tareaService.cambiarEstado(2, "En progreso", 10);

        assertEquals("En progreso", resultado.getEstado());
        verify(tareaRepository).save(tarea);
        verify(historialTareaService).registrarAccion(eq(2), eq(10), contains("En progreso"));
    }

    @Test
    void registrarAvance_ActualizaAvanceYRegistraHistorialSiEstaEnProgreso() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(3);
        tarea.setEstado("En progreso");
        tarea.setAvance((short) 10);

        RegistroAvanceRequestDTO request = new RegistroAvanceRequestDTO();
        request.setAvance((short) 60);
        request.setObservacion("Lote clasificado");

        when(tareaRepository.findById(3)).thenReturn(Optional.of(tarea));
        when(tareaRepository.save(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tarea resultado = tareaService.registrarAvance(3, request, 10);

        assertEquals((short) 60, resultado.getAvance());
        verify(tareaRepository).save(tarea);
        verify(historialTareaService).registrarAccion(eq(3), eq(10), contains("60%"));
        verify(historialTareaService).registrarAccion(eq(3), eq(10), contains("Lote clasificado"));
    }

    @Test
    void registrarAvance_LanzaExcepcionSiNoEstaEnProgreso() {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(4);
        tarea.setEstado("Pendiente");

        RegistroAvanceRequestDTO request = new RegistroAvanceRequestDTO();
        request.setAvance((short) 50);

        when(tareaRepository.findById(4)).thenReturn(Optional.of(tarea));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> {
            tareaService.registrarAvance(4, request, 10);
        });

        assertTrue(ex.getMessage().contains("Solo se puede registrar avance en tareas que están 'En progreso'"));
        verify(historialTareaService, never()).registrarAccion(any(), any(), any());
    }
}