package com.example.backend.dto;

import com.example.backend.model.Rol;
import com.example.backend.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Integer idUsuario,
        String nombre,
        String email,
        Rol rol,
        Boolean activo,
        LocalDateTime fechaCreacion) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getFechaCreacion());
    }
}
