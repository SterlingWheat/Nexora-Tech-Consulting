package com.example.backend.service.impl;

import com.example.backend.dto.ConteoPorEstadoDTO;
import com.example.backend.dto.DashboardMetricsDTO;
import com.example.backend.model.Tarea;
import com.example.backend.repository.AsignacionTareaRepository;
import com.example.backend.repository.AsignacionTurnoRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final VoluntarioRepository voluntarioRepository;
    private final TareaRepository tareaRepository;
    private final TurnoRepository turnoRepository;
    private final AsignacionTareaRepository asignacionTareaRepository;
    private final AsignacionTurnoRepository asignacionTurnoRepository;
    private final Clock clock;

    public DashboardServiceImpl(
            VoluntarioRepository voluntarioRepository,
            TareaRepository tareaRepository,
            TurnoRepository turnoRepository,
            AsignacionTareaRepository asignacionTareaRepository,
            AsignacionTurnoRepository asignacionTurnoRepository,
            Clock clock) {
        this.voluntarioRepository = voluntarioRepository;
        this.tareaRepository = tareaRepository;
        this.turnoRepository = turnoRepository;
        this.asignacionTareaRepository = asignacionTareaRepository;
        this.asignacionTurnoRepository = asignacionTurnoRepository;
        this.clock = clock;
    }

    // RF-44 Criterio 1: los 4 indicadores clave, más el desglose de tareas por estado para el gráfico
    @Override
    @Transactional(readOnly = true)
    public DashboardMetricsDTO obtenerMetricas() {
        long totalVoluntarios = voluntarioRepository.count();
        long voluntariosActivos = voluntarioRepository.contarActivos();

        List<ConteoPorEstadoDTO> tareasPorEstado = tareaRepository.contarPorEstado().stream()
                .map(fila -> new ConteoPorEstadoDTO(fila.getEstado(), fila.getCantidad()))
                .collect(Collectors.toList());

        long turnosProximos = turnoRepository.countByFechaGreaterThanEqualAndEstadoNot(
                LocalDate.now(clock), "CANCELADO");

        // Bug corregido: antes esto no excluía las asignaciones de tareas ya canceladas/finalizadas
        // (al revés de asignacionTurnoRepository.countPendientes(), que sí excluye turnos cancelados),
        // así que una tarea cancelada con una asignación sin responder inflaba este KPI para siempre.
        long asignacionesPendientes = asignacionTareaRepository.countPendientes(Tarea.ESTADOS_TERMINALES)
                + asignacionTurnoRepository.countPendientes();

        return new DashboardMetricsDTO(
                voluntariosActivos,
                totalVoluntarios - voluntariosActivos,
                tareaRepository.contarAbiertas(Tarea.ESTADOS_TERMINALES),
                tareasPorEstado,
                turnosProximos,
                asignacionesPendientes);
    }
}
