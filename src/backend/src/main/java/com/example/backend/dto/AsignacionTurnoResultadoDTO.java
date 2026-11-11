package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionTurnoResultadoDTO {
    private List<AsignacionTurnoResponseDTO> asignaciones;
    // KAN-31 Criterio 3: avisos cuando un voluntario asignado no tiene disponibilidad declarada en esa franja
    private List<String> advertencias;
}
