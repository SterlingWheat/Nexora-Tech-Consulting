package com.example.backend.service;

import com.example.backend.model.Turno;

import java.util.List;

public interface TurnoService {

    // Criterio 1 y 2: crea el turno con sus cupos; rechaza fecha pasada u horario inválido
    Turno crearTurno(Turno turno);

    // Criterio 3: los turnos exponen cupos ocupados y libres
    List<Turno> listarTurnos();

    Turno obtenerTurnoPorId(Integer idTurno);
}
