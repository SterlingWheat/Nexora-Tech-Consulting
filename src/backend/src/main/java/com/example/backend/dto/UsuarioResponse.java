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
        Rol rolEnum = null;
        if (usuario.getRol() != null && usuario.getRol().getNombre() != null) {
            try {
                rolEnum = Rol.valueOf(usuario.getRol().getNombre().toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                rolEnum = null;
            }
        }

        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getEmail(),
                rolEnum,
                usuario.getActivo(),
                usuario.getFechaCreacion());
    }
}