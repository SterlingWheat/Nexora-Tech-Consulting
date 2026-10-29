package com.example.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistroAvanceRequestDTO {

    @NotNull(message = "El porcentaje de avance es obligatorio")
    @Min(value = 0, message = "El avance mínimo es 0%")
    @Max(value = 100, message = "El avance máximo es 100%")
    private Short avance;

    private String observacion;
}