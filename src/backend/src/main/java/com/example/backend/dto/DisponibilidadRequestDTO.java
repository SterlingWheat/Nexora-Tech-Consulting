package com.example.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DisponibilidadRequestDTO {

    // Reemplaza por completo la disponibilidad del voluntario; una lista vacía la borra.
    @NotNull(message = "La lista de franjas es obligatoria")
    @Size(max = 50, message = "No se pueden registrar más de 50 franjas")
    private List<@Valid FranjaDTO> franjas;
}
