package com.example.backend.dto;

import java.time.LocalDateTime;

public record AsignacionTareaMovilDTO(
        Integer idAsignacion,
        Integer idTarea,
        String titulo,
        String descripcion,
        String estado,
        LocalDateTime fechaAsignacion) {
}
