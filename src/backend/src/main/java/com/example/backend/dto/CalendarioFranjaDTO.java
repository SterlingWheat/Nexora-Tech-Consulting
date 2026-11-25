package com.example.backend.dto;

import com.example.backend.model.DiaSemana;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarioFranjaDTO {
    private Integer idVoluntario;
    private String nombreVoluntario;
    private String apellidoVoluntario;
    private LocalDate fecha;
    private DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
