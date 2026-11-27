package com.example.backend.dto;

import jakarta.validation.constraints.Pattern;

// Nombre, apellido y correo no son editables desde este autoservicio (ver VoluntarioService.actualizarPerfil)
public record PerfilMovilUpdateRequest(
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,20}$", message = "El teléfono no tiene un formato válido")
        String telefono) {
}
