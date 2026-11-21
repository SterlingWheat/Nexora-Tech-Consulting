package com.example.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AsignacionTurnoMovilDTO(
        Long idAsignacion,
        Integer idTurno,
        String actividad,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String estado) {
}
