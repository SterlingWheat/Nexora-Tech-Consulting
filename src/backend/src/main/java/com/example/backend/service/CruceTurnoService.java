package com.example.backend.service;

import com.example.backend.model.Turno;

import java.util.List;

public interface CruceTurnoService {

    /** Turnos vigentes del voluntario que se cruzan con el turno indicado (vacío si no hay cruce). */
    List<Turno> buscarCruces(Integer idVoluntario, Integer idTurno);

    /**
     * Criterio 1 y 2: bloquea (ConflictException) si el voluntario ya tiene un turno que se cruza, e indica con
     * cuál choca. Debe invocarse antes de guardar una asignación voluntario-turno (KAN-31, KAN-43).
     */
    void validarSinCruces(Integer idVoluntario, Integer idTurno);
}
