package com.example.backend.service;

import com.example.backend.dto.DashboardMetricsDTO;
import com.example.backend.repository.AsignacionTareaRepository;
import com.example.backend.repository.AsignacionTurnoRepository;
import com.example.backend.repository.ConteoPorEstado;
import com.example.backend.repository.TareaRepository;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // "Hoy" fijo: 2026-11-23
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-23T14:00:00Z"), ZONA);

    @Mock
    private VoluntarioRepository voluntarioRepository;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private AsignacionTareaRepository asignacionTareaRepository;
    @Mock
    private AsignacionTurnoRepository asignacionTurnoRepository;

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(
                voluntarioRepository, tareaRepository, turnoRepository,
                asignacionTareaRepository, asignacionTurnoRepository, RELOJ);
    }

    private static ConteoPorEstado conteo(String estado, long cantidad) {
        return new ConteoPorEstado() {
            @Override public String getEstado() { return estado; }
            @Override public Long getCantidad() { return cantidad; }
        };
    }

    // RF-44 Criterio 1
    @Test
    void obtenerMetricas_devuelveLosCuatroIndicadoresClave() {
        when(voluntarioRepository.count()).thenReturn(20L);
        when(voluntarioRepository.contarActivos()).thenReturn(15L);
        when(tareaRepository.contarAbiertas()).thenReturn(7L);
        when(tareaRepository.contarPorEstado()).thenReturn(List.of(
                conteo("Pendiente", 3L), conteo("En progreso", 4L), conteo("Finalizada", 10L)));
        when(turnoRepository.countByFechaGreaterThanEqualAndEstadoNot(LocalDate.of(2026, 11, 23), "CANCELADO"))
                .thenReturn(5L);
        when(asignacionTareaRepository.countPendientes()).thenReturn(2L);
        when(asignacionTurnoRepository.countPendientes()).thenReturn(3);

        DashboardMetricsDTO metricas = service.obtenerMetricas();

        assertEquals(15L, metricas.voluntariosActivos());
        assertEquals(5L, metricas.voluntariosInactivos());
        assertEquals(7L, metricas.tareasAbiertas());
        assertEquals(5L, metricas.turnosProximos());
        // asignaciones pendientes = tareas pendientes de respuesta + turnos pendientes de respuesta
        assertEquals(5L, metricas.asignacionesPendientes());
    }

    @Test
    void obtenerMetricas_incluyeElDesglosePorEstadoParaElGrafico() {
        when(voluntarioRepository.count()).thenReturn(1L);
        when(voluntarioRepository.contarActivos()).thenReturn(1L);
        when(tareaRepository.contarPorEstado()).thenReturn(List.of(conteo("Pendiente", 3L)));
        when(turnoRepository.countByFechaGreaterThanEqualAndEstadoNot(LocalDate.of(2026, 11, 23), "CANCELADO"))
                .thenReturn(0L);

        DashboardMetricsDTO metricas = service.obtenerMetricas();

        assertEquals(1, metricas.tareasPorEstado().size());
        assertEquals("Pendiente", metricas.tareasPorEstado().get(0).estado());
        assertEquals(3L, metricas.tareasPorEstado().get(0).cantidad());
    }

    @Test
    void obtenerMetricas_sinVoluntariosInactivos_noQuedaNegativo() {
        when(voluntarioRepository.count()).thenReturn(5L);
        when(voluntarioRepository.contarActivos()).thenReturn(5L);
        when(tareaRepository.contarPorEstado()).thenReturn(List.of());
        when(turnoRepository.countByFechaGreaterThanEqualAndEstadoNot(LocalDate.of(2026, 11, 23), "CANCELADO"))
                .thenReturn(0L);

        DashboardMetricsDTO metricas = service.obtenerMetricas();

        assertEquals(0L, metricas.voluntariosInactivos());
    }
}
