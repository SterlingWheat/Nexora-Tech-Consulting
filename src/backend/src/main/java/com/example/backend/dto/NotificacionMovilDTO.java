package com.example.backend.dto;

import java.time.LocalDateTime;

public record NotificacionMovilDTO(
        Long id,
        String tipo,
        String actividad,
        String fechaActividad,
        LocalDateTime fechaAviso,
        boolean leida) {
}
