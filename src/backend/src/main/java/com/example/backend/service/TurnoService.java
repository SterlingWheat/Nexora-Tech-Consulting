package com.example.backend.service;

import com.example.backend.model.Turno;

import java.time.LocalDate;
import java.util.List;

public interface TurnoService {

    // Criterio 1 y 2 (Creación) + Criterio 3 (Auditoría)
    Turno crearTurno(Turno turno, Integer idUsuarioGestor);

    Turno actualizarTurno(Integer idTurno, Turno turno, Integer idUsuarioGestor);

    Turno cancelarTurno(Integer idTurno, Integer idUsuarioGestor);

    List<Turno> listarTurnos();

    Turno obtenerTurnoPorId(Integer idTurno);

    // RF-30 (KAN-53) Criterios 1 y 2: turnos por rango de fechas (día/semana/mes), con filtro
    // opcional por actividad o voluntario, para que el calendario general resalte la cobertura
    List<Turno> listarTurnos(LocalDate desde, LocalDate hasta, String actividad, Integer idVoluntario);
}