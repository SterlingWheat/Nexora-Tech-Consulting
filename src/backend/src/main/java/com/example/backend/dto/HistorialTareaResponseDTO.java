package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistorialTareaResponseDTO {
    private Integer idHistorial;
    private Integer idTarea;
    private Integer idUsuario;
    private String nombreUsuario;
    
    // HU2: Nuevos campos expuestos para el componente HistorialTareaTimeline.vue
    private String tipoCambio;
    private String detalle;
    private String valorAnterior;
    private String valorNuevo;
    
    private LocalDateTime fechaRegistro;
}