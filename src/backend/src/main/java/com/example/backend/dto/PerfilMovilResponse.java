package com.example.backend.dto;

public record PerfilMovilResponse(
        Integer idVoluntario,
        String nombre,
        String apellido,
        String documento,
        String email,
        String telefono,
        String rol) {
}
