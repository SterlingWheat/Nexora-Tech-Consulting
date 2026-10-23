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

    @Column(name = "cupos_totales", nullable = false)
    private Integer cuposTotales;

    // Lo incrementarán las asignaciones de voluntarios a turnos (historias futuras)
    @Column(name = "cupos_ocupados", nullable = false)
    @Builder.Default
    private Integer cuposOcupados = 0;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.cuposOcupados == null) {
            this.cuposOcupados = 0;
        }
    }

    // Criterio 3: cupos libres = totales - ocupados
    @Transient
    public int getCuposLibres() {
        return cuposTotales - cuposOcupados;
    }
}
