package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadResponseDTO {
    private Integer idVoluntario;
    private List<FranjaDTO> franjas;
    // Criterio 3: avisos cuando se unifican franjas superpuestas
    private List<String> advertencias;
}
