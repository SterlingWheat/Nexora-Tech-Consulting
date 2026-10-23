package com.example.backend.controller;

import com.example.backend.model.Usuario;
import com.example.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario usuario) {
        // Validar que el correo no esté registrado
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            return ResponseEntity.badRequest().body("El correo ya está registrado.");
        }
        usuario.setActivo(true); // Por defecto activo
        return ResponseEntity.ok(usuarioRepository.save(usuario));
    }

    
    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo) {
        
        List<Usuario> usuarios = usuarioRepository.findAll();

        if (rol != null && !rol.isEmpty()) {
            usuarios = usuarios.stream()
                    .filter(u -> u.getRol().name().equalsIgnoreCase(rol))
                    .toList();
        }
        

        if (activo != null) {
            usuarios = usuarios.stream()
                    .filter(u -> u.getActivo().equals(activo))
                    .toList();
        }
        
        return ResponseEntity.ok(usuarios);
    }


    @PutMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivarUsuario(@PathVariable Integer id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Usuario usuario = usuarioOpt.get();
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok("Usuario desactivado correctamente.");
    }
}