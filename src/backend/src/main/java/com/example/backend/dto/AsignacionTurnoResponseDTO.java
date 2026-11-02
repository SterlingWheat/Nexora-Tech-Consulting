package com.example.backend.dto;

import lombok.Data;

@Data
public class AsignacionTurnoResponseDTO {

    private Long idAsignacion;
    private Integer idTurno;
    private Integer idVoluntario;
    private String nombreVoluntario;
    private String estado;
}
