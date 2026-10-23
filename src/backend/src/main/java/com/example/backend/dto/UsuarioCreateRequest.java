package com.example.backend.dto;

import com.example.backend.model.Rol;

public record UsuarioCreateRequest(
        String nombre,
        String email,
        String password,
        Rol rol) {
}
