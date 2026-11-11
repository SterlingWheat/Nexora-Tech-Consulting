package com.example.backend.service;

import com.example.backend.model.Turno;

import java.util.List;

public interface TurnoService {

    // Criterio 1 y 2 (Creación) + Criterio 3 (Auditoría)
    Turno crearTurno(Turno turno, Integer idUsuarioGestor);

    Turno actualizarTurno(Integer idTurno, Turno turno, Integer idUsuarioGestor);

    Turno cancelarTurno(Integer idTurno, Integer idUsuarioGestor);

    List<Turno> listarTurnos();

    Turno obtenerTurnoPorId(Integer idTurno);
}