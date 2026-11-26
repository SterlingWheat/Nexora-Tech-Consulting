package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_tarea")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer idHistorial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tarea", nullable = false)
    private Tarea tarea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // HU2: Tipo de cambio para el frontend (ej. CREACION, ESTADO, ASIGNACION, AVANCE)
    @Column(name = "tipo_cambio", length = 50)
    private String tipoCambio;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    // HU2: Valores para auditoría de cambios específicos (opcionales)
    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
        // Asignar tipo genérico si llega por un método antiguo
        if (this.tipoCambio == null) {
            this.tipoCambio = "EDICION";
        }
    }
}