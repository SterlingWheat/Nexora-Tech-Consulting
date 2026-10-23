package com.example.backend.controller;

import com.example.backend.model.Usuario;
import com.example.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/voluntarios")
public class VoluntarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPerfil(@PathVariable Integer id, Authentication authentication) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOpt.get();

        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTARIO"))) {
            

            String usernameAutenticado = authentication.getName();
            
            if (!usuario.getEmail().equals(usernameAutenticado)) {
                return ResponseEntity.status(403).body("Acceso denegado: no puedes ver el perfil de otro voluntario.");
            }
        }

        return ResponseEntity.ok(usuario);
    }
}