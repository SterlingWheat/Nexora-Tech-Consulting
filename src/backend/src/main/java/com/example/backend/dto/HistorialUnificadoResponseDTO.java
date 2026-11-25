package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialUnificadoResponseDTO {
    
    private Long participacionId; // ID del HistorialTarea o HistorialTurno original
    private String tipo; // Identificador visual: "TAREA" o "TURNO"
    private String nombre; // Nombre de la tarea o nombre/descripción del turno
    private LocalDate fecha; 
    private LocalTime horaInicio; 
    private LocalTime horaFin; 
    private String estado; // Estado histórico (ej. COMPLETADA, NO_ASISTIO, CANCELADA)
    private String observaciones; // Comentarios de cierre o avance si aplica
}