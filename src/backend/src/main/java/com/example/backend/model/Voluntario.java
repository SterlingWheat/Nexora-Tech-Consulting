package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(unique = true, length = 30)
    private String documento;

    @Column(unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(name = "carrera_profesion", length = 100)
    private String carreraProfesion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // Relación añadida para HU2: Catálogo de habilidades
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "voluntario_habilidad",
        joinColumns = @JoinColumn(name = "voluntario_id", referencedColumnName = "id_voluntario"),
        inverseJoinColumns = @JoinColumn(name = "habilidad_id", referencedColumnName = "id")
    )
    @Builder.Default
    private Set<Habilidad> habilidades = new HashSet<>();

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