package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarioDisponibilidadResponseDTO {
    private LocalDate desde;
    private LocalDate hasta;
    private List<CalendarioFranjaDTO> franjas;
}
