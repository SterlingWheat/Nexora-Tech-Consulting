package com.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String email;

    // Corregido al nombre exacto de la base de datos: password_hash
    @Column(name = "password_hash", nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private RolEntity rol;

    // Mapeado como String porque en PostgreSQL la columna estado es character varying
    @Column(name = "estado", length = 30)
    @Builder.Default
    private String estado = "ACTIVO";

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null || this.estado.isBlank()) {
            this.estado = "ACTIVO";
        }
    }

    // Métodos de compatibilidad para que el resto del sistema siga usando booleanos
    public Boolean getActivo() {
        return "ACTIVO".equalsIgnoreCase(this.estado) || "TRUE".equalsIgnoreCase(this.estado);
    }

    public void setActivo(Boolean activo) {
        this.estado = (activo != null && activo) ? "ACTIVO" : "INACTIVO";
    }
}