package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tarea")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarea {

    // Única fuente de verdad para los estados terminales de una tarea: antes estaban repetidos
    // como literales en TareaServiceImpl (editarTarea, cambiarEstado) y en TareaRepository
    // (contarAbiertas, filtrarTareas), con riesgo de desincronizarse si el flujo de estados cambia.
    public static final List<String> ESTADOS_TERMINALES = List.of("Finalizada", "Cancelada");

    public static boolean esEstadoTerminal(String estado) {
        return ESTADOS_TERMINALES.stream().anyMatch(e -> e.equalsIgnoreCase(estado));
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea")
    private Integer idTarea;

    @Column(nullable = false, length = 200)
    private String titulo;

    // Mapeado explícitamente al error tipográfico "descripcior" de la tabla para evitar fallos SQL
    @Column(name = "descripcior", columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaTarea categoria;

    @Column(length = 30)
    private String estado;

    @Column(name = "avance")
    private Short avance;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null || this.estado.isEmpty()) {
            this.estado = "Pendiente";
        }
        if (this.avance == null) {
            this.avance = 0;
        }
    }
}