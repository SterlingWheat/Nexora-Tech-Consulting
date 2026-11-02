package com.example.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class AsignacionTurnoRequestDTO {

    @NotEmpty(message = "Debe proporcionar al menos el ID de un voluntario")
    private List<@NotNull @Positive Integer> idVoluntarios;
}
