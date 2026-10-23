package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VoluntarioResponseDTO {
    private Integer idVoluntario;
    private String nombre;
    private String apellido;
    private String documento;
    private String email;
    private String telefono;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
}
