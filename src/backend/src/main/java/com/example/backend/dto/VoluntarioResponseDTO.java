package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoluntarioResponseDTO {
    private Integer idVoluntario;
    private Integer idUsuario; // Identificador para saber si tiene cuenta de sistema
    private String nombre;
    private String apellido;
    private String documento;
    private String email;
    private String telefono;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    // Campo añadido para HU2: Retorna los detalles de las habilidades asignadas
    private List<HabilidadDTO> habilidades;
}