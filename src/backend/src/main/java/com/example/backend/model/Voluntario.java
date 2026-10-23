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

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 30)
    private String documento;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
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

    // Criterio 3: queda registro de la fecha de modificación
    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
