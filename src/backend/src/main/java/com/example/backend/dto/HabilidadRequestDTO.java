package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HabilidadRequestDTO {
    @NotBlank(message = "El nombre de la habilidad es obligatorio")
    @Size(max = 100, message = "El nombre de la habilidad no puede superar los 100 caracteres")
    private String nombre;
}
