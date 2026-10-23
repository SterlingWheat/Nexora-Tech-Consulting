package com.example.backend.service.impl;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Turno;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.service.TurnoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TurnoServiceImpl implements TurnoService {

    private final TurnoRepository turnoRepository;
    private final Clock clock;

    public TurnoServiceImpl(TurnoRepository turnoRepository, Clock clock) {
        this.turnoRepository = turnoRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Turno crearTurno(Turno turno) {
        turno.setActividad(turno.getActividad().trim());
        validar(turno);
        // Criterio 1: el turno nace con todos sus cupos libres
        turno.setCuposOcupados(0);
        return turnoRepository.save(turno);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Turno> listarTurnos() {
        return turnoRepository.findAllByOrderByFechaAscHoraInicioAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Turno obtenerTurnoPorId(Integer idTurno) {
        return turnoRepository.findById(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con ID: " + idTurno));
    }

    // Criterio 2
    private void validar(Turno t) {
        if (!t.getHoraFin().isAfter(t.getHoraInicio())) {
            throw new BusinessValidationException("La hora de fin debe ser posterior a la hora de inicio");
        }
        LocalDate hoy = LocalDate.now(clock);
        if (t.getFecha().isBefore(hoy)) {
            throw new BusinessValidationException("La fecha del turno no puede ser pasada");
        }
        // Un turno de hoy cuyo horario ya terminó equivale a una fecha pasada
        if (!LocalDateTime.of(t.getFecha(), t.getHoraFin()).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessValidationException("El horario del turno ya terminó");
        }
    }
}
