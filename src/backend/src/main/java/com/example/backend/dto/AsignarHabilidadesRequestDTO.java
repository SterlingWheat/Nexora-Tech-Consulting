package com.example.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AsignarHabilidadesRequestDTO {
    @NotNull(message = "La lista de habilidades es obligatoria")
    private List<Integer> idsHabilidades;
}
