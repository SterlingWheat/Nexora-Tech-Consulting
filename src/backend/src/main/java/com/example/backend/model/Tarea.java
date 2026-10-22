package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarea")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea")
    private Integer idTarea; /*[cite: 2] */

    @Column(nullable = false, length = 200)
    private String titulo; /*[cite: 2] */

    @Column(columnDefinition = "TEXT")
    private String descripcion; /*[cite: 2] */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaTarea categoria; 

    @Column(length = 30)
    private String estado; /*[cite: 2] */

    @Column(name = "avance")
    private Short avance; /*[cite: 2] */

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion; /*[cite: 2] */

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null || this.estado.isEmpty()) {
            this.estado = "Pendiente"; /*[cite: 2] */
        }
        if (this.avance == null) {
            this.avance = 0; /*[cite: 2] */
        }
    }
}