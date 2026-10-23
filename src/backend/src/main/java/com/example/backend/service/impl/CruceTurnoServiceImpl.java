package com.example.backend.service.impl;

import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Turno;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.CruceTurnoService;
import com.example.backend.service.ReglaCruceHorario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CruceTurnoServiceImpl implements CruceTurnoService {

    private final TurnoRepository turnoRepository;
    private final VoluntarioRepository voluntarioRepository;

    public CruceTurnoServiceImpl(TurnoRepository turnoRepository, VoluntarioRepository voluntarioRepository) {
        this.turnoRepository = turnoRepository;
        this.voluntarioRepository = voluntarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Turno> buscarCruces(Integer idVoluntario, Integer idTurno) {
        if (!voluntarioRepository.existsById(idVoluntario)) {
            throw new ResourceNotFoundException("Voluntario no encontrado con ID: " + idVoluntario);
        }
        Turno nuevo = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con ID: " + idTurno));

        return turnoRepository.buscarTurnosVigentesDelVoluntarioEnFecha(idVoluntario, nuevo.getFecha(), idTurno).stream()
                .filter(existente -> ReglaCruceHorario.seCruzan(existente, nuevo))
                .sorted(Comparator.comparing(Turno::getHoraInicio))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public void validarSinCruces(Integer idVoluntario, Integer idTurno) {
        List<Turno> cruces = buscarCruces(idVoluntario, idTurno);
        if (!cruces.isEmpty()) {
            throw new ConflictException("Cruce de horarios: el voluntario ya está asignado a "
                    + cruces.stream().map(this::describir).collect(Collectors.joining("; ")));
        }
    }

    // Criterio 2: el mensaje indica el turno con el que choca
    private String describir(Turno t) {
        return "«" + t.getActividad() + "» el " + t.getFecha() + " de " + t.getHoraInicio() + " a " + t.getHoraFin();
    }
}
