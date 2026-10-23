package com.example.backend.controller;

import com.example.backend.dto.HabilitarAccesoRequestDTO;
import com.example.backend.dto.VoluntarioRequestDTO;
import com.example.backend.dto.VoluntarioResponseDTO;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.VoluntarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/voluntarios")
@CrossOrigin(origins = "*")
public class VoluntarioGestionController {

    private final VoluntarioService voluntarioService;
    private final UsuarioRepository usuarioRepository;

    public VoluntarioGestionController(VoluntarioService voluntarioService, UsuarioRepository usuarioRepository) {
        this.voluntarioService = voluntarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public ResponseEntity<VoluntarioResponseDTO> registrarVoluntario(@Valid @RequestBody VoluntarioRequestDTO request) {
        Voluntario guardado = voluntarioService.registrarVoluntario(
                mapearAEntidad(request), 
                request.getCrearCuenta(), 
                request.getPassword()
        );
        return new ResponseEntity<>(mapearADTO(guardado), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoluntarioResponseDTO> actualizarVoluntario(@PathVariable Integer id, @Valid @RequestBody VoluntarioRequestDTO request) {
        return ResponseEntity.ok(mapearADTO(voluntarioService.actualizarVoluntario(id, mapearAEntidad(request))));
    }

    // Nuevo Endpoint para el botón Habilitar Acceso
    @PostMapping("/{id}/habilitar-acceso")
    public ResponseEntity<VoluntarioResponseDTO> habilitarAcceso(
            @PathVariable Integer id, 
            @Valid @RequestBody HabilitarAccesoRequestDTO request) {
        Voluntario actualizado = voluntarioService.habilitarAcceso(id, request.getPassword());
        return ResponseEntity.ok(mapearADTO(actualizado));
    }

    @GetMapping
    public ResponseEntity<List<VoluntarioResponseDTO>> listarVoluntarios() {
        List<VoluntarioResponseDTO> voluntarios = voluntarioService.listarVoluntarios().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(voluntarios);
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<VoluntarioResponseDTO> obtenerVoluntario(@PathVariable Integer id) {
        return ResponseEntity.ok(mapearADTO(voluntarioService.obtenerVoluntarioPorId(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPerfilUsuarioDelVoluntario(@PathVariable Integer id, Authentication authentication) {
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

    private Voluntario mapearAEntidad(VoluntarioRequestDTO request) {
        Voluntario v = new Voluntario();
        v.setNombre(request.getNombre());
        v.setApellido(request.getApellido());
        v.setDocumento(request.getDocumento());
        v.setEmail(request.getEmail());
        v.setTelefono(request.getTelefono());
        return v;
    }

    private VoluntarioResponseDTO mapearADTO(Voluntario v) {
        VoluntarioResponseDTO dto = new VoluntarioResponseDTO();
        dto.setIdVoluntario(v.getIdVoluntario());
        // Incluir el id_usuario si lo tiene
        dto.setIdUsuario(v.getUsuario() != null ? v.getUsuario().getIdUsuario() : null);
        dto.setNombre(v.getNombre());
        dto.setApellido(v.getApellido());
        dto.setDocumento(v.getDocumento());
        dto.setEmail(v.getEmail());
        dto.setTelefono(v.getTelefono());
        dto.setActivo(v.getActivo());
        dto.setFechaCreacion(v.getFechaCreacion());
        dto.setFechaModificacion(v.getFechaModificacion());
        return dto;
    }
}