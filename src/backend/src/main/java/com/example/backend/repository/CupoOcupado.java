package com.example.backend.repository;

/** Proyección: cupos ocupados de un turno, contados desde asignacion_turno. */
public interface CupoOcupado {
    Integer getIdTurno();

    Long getTotal();
}
