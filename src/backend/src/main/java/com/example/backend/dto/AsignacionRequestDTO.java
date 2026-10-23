package com.example.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AsignacionRequestDTO {
    @NotEmpty(message = "Debe proporcionar al menos el ID de un voluntario")
    private List<@NotNull Integer> idVoluntarios;
}