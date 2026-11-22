package com.example.backend.controller;

import com.example.backend.dto.PerfilMovilResponse;
import com.example.backend.dto.PerfilMovilUpdateRequest;
import com.example.backend.dto.PerfilMovilUpdateResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.JwtService;
import com.example.backend.service.VoluntarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movil/perfil")
public class PerfilMovilController {

    private final UsuarioRepository usuarioRepository;
    private final VoluntarioRepository voluntarioRepository;
    private final VoluntarioService voluntarioService;
    private final JwtService jwtService;

    public PerfilMovilController(
            UsuarioRepository usuarioRepository,
            VoluntarioRepository voluntarioRepository,
            VoluntarioService voluntarioService,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.voluntarioRepository = voluntarioRepository;
        this.voluntarioService = voluntarioService;
        this.jwtService = jwtService;
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilMovilResponse> obtenerMiPerfil(Authentication authentication) {
        Usuario usuario = usuarioAutenticado(authentication);
        Voluntario voluntario = voluntarioAutenticado(usuario);
        return ResponseEntity.ok(aResponse(usuario, voluntario));
    }

    @PutMapping("/me")
    public ResponseEntity<PerfilMovilUpdateResponse> actualizarMiPerfil(
            @Valid @RequestBody PerfilMovilUpdateRequest request,
            Authentication authentication) {
        Usuario usuario = usuarioAutenticado(authentication);
        Voluntario actualizado = voluntarioService.actualizarPerfil(
                usuario.getIdUsuario(),
                request.nombre(),
                request.apellido(),
                request.email(),
                request.telefono());
        Usuario usuarioActualizado = usuarioRepository.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la cuenta de acceso del voluntario."));
        String rol = usuarioActualizado.getRol().getNombre().trim().toUpperCase();
        String token = jwtService.generarToken(usuarioActualizado.getEmail(), rol);
        return ResponseEntity.ok(new PerfilMovilUpdateResponse(
                aResponse(usuarioActualizado, actualizado), token));
    }

    private Usuario usuarioAutenticado(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado en el sistema."));
    }

    private Voluntario voluntarioAutenticado(Usuario usuario) {
        return voluntarioRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el perfil de voluntario para el usuario autenticado."));
    }

    private PerfilMovilResponse aResponse(Usuario usuario, Voluntario voluntario) {
        return new PerfilMovilResponse(
                voluntario.getIdVoluntario(),
                voluntario.getNombre(),
                voluntario.getApellido(),
                voluntario.getDocumento(),
                voluntario.getEmail(),
                voluntario.getTelefono(),
                usuario.getRol().getNombre());
    }
}
