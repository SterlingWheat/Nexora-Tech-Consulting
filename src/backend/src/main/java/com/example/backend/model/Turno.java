package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "turno", indexes = @Index(name = "idx_turno_fecha", columnList = "fecha"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno")
    private Integer idTurno;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false, length = 200)
    private String actividad;

    // Columna "cupos" del esquema original del equipo
    @Column(name = "cupos", nullable = false)
    private Integer cuposTotales;

    // No se guarda: se cuenta desde asignacion_turno para que las asignaciones del resto del equipo se reflejen solas
    @Transient
    @Builder.Default
    private Integer cuposOcupados = 0;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }

    // Criterio 3: cupos libres = totales - ocupados
    @Transient
    public int getCuposLibres() {
        return cuposTotales - cuposOcupados;
    }
}
