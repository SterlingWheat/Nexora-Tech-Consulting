package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TareaResponseDTO {
    private Integer idTarea;
    private String titulo;
    private String descripcion;
    private Integer idCategoria;
    private String nombreCategoria;
    private String estado;
    private Short avance;
    private LocalDateTime fechaCreacion;
}