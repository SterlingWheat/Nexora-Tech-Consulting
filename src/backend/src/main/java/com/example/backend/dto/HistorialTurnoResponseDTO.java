package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistorialTurnoResponseDTO {
    private Integer idHistorial;
    private Integer idTurno;
    private Integer idUsuario;
    private String nombreUsuario;
    private String detalle;
    private LocalDateTime fechaRegistro;
}