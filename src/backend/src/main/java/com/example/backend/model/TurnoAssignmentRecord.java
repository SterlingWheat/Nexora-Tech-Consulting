package com.example.backend.model;

public record TurnoAssignmentRecord(
        Long idAsignacion,
        Integer idTurno,
        Integer idVoluntario,
        String nombreVoluntario,
        String email,
        String estado) {
}
