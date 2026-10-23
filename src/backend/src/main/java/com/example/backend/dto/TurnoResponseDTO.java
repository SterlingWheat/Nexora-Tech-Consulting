package com.example.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TurnoResponseDTO {
    private Integer idTurno;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String actividad;
    private Integer cuposTotales;
    private Integer cuposOcupados;
    private Integer cuposLibres;
    private LocalDateTime fechaCreacion;
}
