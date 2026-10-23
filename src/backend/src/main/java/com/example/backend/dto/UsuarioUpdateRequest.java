package com.example.backend.dto;

import com.example.backend.model.Rol;

public record UsuarioUpdateRequest(
        String nombre,
        String email,
        String password,
        Rol rol) {
}
