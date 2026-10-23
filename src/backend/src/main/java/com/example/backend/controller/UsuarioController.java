package com.example.backend.controller;

import com.example.backend.dto.UsuarioCreateRequest;
import com.example.backend.dto.UsuarioResponse;
import com.example.backend.dto.UsuarioUpdateRequest;
import com.example.backend.model.RolEntity;
import com.example.backend.model.Usuario;
import com.example.backend.repository.RolRepository;
import com.example.backend.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioCreateRequest request) {
        if (request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()
                || request.password().length() < 8
                || request.nombre() == null || request.nombre().isBlank()
                || request.rol() == null) {
            return ResponseEntity.badRequest().body("Datos de usuario inválidos.");
        }

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(409).body("El correo ya está registrado.");
        }

        String nombreRol = request.rol().name();
        RolEntity rolEntity = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("El rol " + nombreRol + " no existe en la base de datos."));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .rol(rolEntity)
                .estado("ACTIVO") // <--- Corregido a .estado("ACTIVO")
                .build();

        return ResponseEntity.ok(UsuarioResponse.from(usuarioRepository.save(usuario)));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo) {

        List<Usuario> usuarios = usuarioRepository.findAll();

        if (rol != null && !rol.isEmpty()) {
            usuarios = usuarios.stream()
                    .filter(u -> u.getRol().getNombre().equalsIgnoreCase(rol))
                    .toList();
        }

        if (activo != null) {
            usuarios = usuarios.stream()
                    .filter(u -> u.getActivo().equals(activo))
                    .toList();
        }

        return ResponseEntity.ok(usuarios.stream().map(UsuarioResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerUsuario(@PathVariable Integer id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Integer id,
            @RequestBody UsuarioUpdateRequest request) {

        if (request.nombre() == null || request.nombre().isBlank()
                || request.email() == null || request.email().isBlank()
                || (request.password() != null && !request.password().isBlank()
                        && request.password().length() < 8)
                || request.rol() == null) {
            return ResponseEntity.badRequest().body("Datos de usuario inválidos.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdUsuarioNot(email, id)) {
            return ResponseEntity.status(409).body("El correo ya está registrado.");
        }

        String nombreRol = request.rol().name();
        RolEntity rolEntity = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("El rol " + nombreRol + " no existe en la base de datos."));

        Usuario usuario = usuarioOpt.get();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setRol(rolEntity);

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        return ResponseEntity.ok(UsuarioResponse.from(usuarioRepository.save(usuario)));
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

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}