package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistorialTareaResponseDTO {
    private Integer idHistorial;
    private Integer idTarea;
    private Integer idUsuario;
    private String nombreUsuario;
    private String detalle;
    private LocalDateTime fechaRegistro;
}