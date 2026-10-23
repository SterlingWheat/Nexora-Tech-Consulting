package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambioEstadoRequestDTO {
    @NotBlank(message = "El nuevo estado es obligatorio")
    private String nuevoEstado;
}