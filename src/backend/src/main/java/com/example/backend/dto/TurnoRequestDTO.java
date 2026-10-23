package com.example.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TurnoRequestDTO {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @NotBlank(message = "La actividad es obligatoria")
    @Size(max = 200, message = "La actividad no puede superar los 200 caracteres")
    private String actividad;

    @NotNull(message = "Los cupos son obligatorios")
    @Min(value = 1, message = "Los cupos deben ser al menos 1")
    @Max(value = 500, message = "Los cupos no pueden superar 500")
    private Integer cuposTotales;
}
