package com.example.backend.service;

import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.HistorialTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Usuario;
import com.example.backend.repository.HistorialTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.impl.HistorialTareaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistorialTareaServiceImplTest {

    @Mock
    private HistorialTareaRepository historialTareaRepository;

    @Mock
    private TareaRepository tareaRepository;
    
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private HistorialTareaServiceImpl historialTareaService;

    private Tarea tareaMock;
    private Usuario usuarioMock;
    private HistorialTarea historialMock;

    @BeforeEach
    void setUp() {
        tareaMock = new Tarea();
        tareaMock.setIdTarea(1);

        usuarioMock = new Usuario();
        usuarioMock.setIdUsuario(100);
        usuarioMock.setNombre("Gestor Pruebas");

        historialMock = new HistorialTarea();
        historialMock.setIdHistorial(10);
        historialMock.setTarea(tareaMock);
        historialMock.setUsuario(usuarioMock);
        historialMock.setTipoCambio("ESTADO");
        historialMock.setDetalle("Cambio de PENDIENTE a EN_PROGRESO");
        historialMock.setFechaRegistro(LocalDateTime.now());
    }

    @Test
    void obtenerHistorialPorTarea_DebeRetornarLista_CuandoExistenRegistros() {
        // Usamos Integer en lugar de Long
        when(historialTareaRepository.findByTareaIdTareaOrderByFechaRegistroDesc(1))
                .thenReturn(List.of(historialMock));

        List<HistorialTarea> resultado = historialTareaService.obtenerHistorialPorTarea(1);

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        assertEquals("ESTADO", resultado.get(0).getTipoCambio());
        assertEquals("Gestor Pruebas", resultado.get(0).getUsuario().getNombre());

        verify(historialTareaRepository, times(1)).findByTareaIdTareaOrderByFechaRegistroDesc(1);
    }

    @Test
    void registrarAccionDetallada_DebeGuardarHistorialExitosamente() {
        when(tareaRepository.findById(1)).thenReturn(Optional.of(tareaMock));
        when(usuarioRepository.findById(100)).thenReturn(Optional.of(usuarioMock));
        when(historialTareaRepository.save(any(HistorialTarea.class))).thenReturn(historialMock);

        HistorialTarea resultado = historialTareaService.registrarAccionDetallada(
            1, 100, "ESTADO", "Cambio exitoso", "Anterior", "Nuevo"
        );

        assertNotNull(resultado);
        verify(tareaRepository, times(1)).findById(1);
        verify(usuarioRepository, times(1)).findById(100);
        verify(historialTareaRepository, times(1)).save(any(HistorialTarea.class));
    }
    
    @Test
    void registrarAccionDetallada_DebeLanzarExcepcion_CuandoTareaNoExiste() {
        when(tareaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            historialTareaService.registrarAccionDetallada(99, 100, "ESTADO", "Cambio", null, null);
        });

        verify(tareaRepository, times(1)).findById(99);
        verify(historialTareaRepository, never()).save(any());
    }
}