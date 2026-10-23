package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AsignacionResponseDTO {
    private Integer idAsignacion;
    private Integer idTarea;
    private Integer idVoluntario;
    private String nombreVoluntario;
    private String estadoRespuesta;
    private LocalDateTime fechaAsignacion;
}