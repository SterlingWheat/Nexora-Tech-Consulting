package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "voluntario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voluntario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_voluntario")
    private Integer idVoluntario;

    // Agregada la relación con Usuario que figura en el diagrama[cite: 3]
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombre;

    // Corregido para apuntar a la columna en singular "apellido" según la BD[cite: 1, 3]
    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    // Columnas requeridas por el frontend, deben crearse en la BD[cite: 1, 3]
    @Column(unique = true, length = 30)
    private String documento;

    @Column(unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String telefono;

    // Mapeo de la columna carrera_profesion existente en la BD[cite: 3]
    @Column(name = "carrera_profesion", length = 100)
    private String carreraProfesion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // Mapeado a la columna "fecha_registro" de la BD en lugar de fecha_creacion[cite: 3]
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.activo == null) {
            this.activo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}