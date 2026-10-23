package com.example.backend.controller;

import com.example.backend.dto.VoluntarioRequestDTO;
import com.example.backend.dto.VoluntarioResponseDTO;
import com.example.backend.model.Voluntario;
import com.example.backend.service.VoluntarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/voluntarios")
@CrossOrigin(origins = "*")
public class VoluntarioGestionController {

    private final VoluntarioService voluntarioService;

    public VoluntarioGestionController(VoluntarioService voluntarioService) {
        this.voluntarioService = voluntarioService;
    }

    @PostMapping
    public ResponseEntity<VoluntarioResponseDTO> registrarVoluntario(@Valid @RequestBody VoluntarioRequestDTO request) {
        Voluntario guardado = voluntarioService.registrarVoluntario(mapearAEntidad(request));
        return new ResponseEntity<>(mapearADTO(guardado), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoluntarioResponseDTO> actualizarVoluntario(@PathVariable Integer id, @Valid @RequestBody VoluntarioRequestDTO request) {
        return ResponseEntity.ok(mapearADTO(voluntarioService.actualizarVoluntario(id, mapearAEntidad(request))));
    }

    @GetMapping
    public ResponseEntity<List<VoluntarioResponseDTO>> listarVoluntarios() {
        List<VoluntarioResponseDTO> voluntarios = voluntarioService.listarVoluntarios().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(voluntarios);
    }

    // /{id} ya lo usa el perfil de usuario (VoluntarioController, KAN-16), por eso este va en /{id}/detalle
    @GetMapping("/{id}/detalle")
    public ResponseEntity<VoluntarioResponseDTO> obtenerVoluntario(@PathVariable Integer id) {
        return ResponseEntity.ok(mapearADTO(voluntarioService.obtenerVoluntarioPorId(id)));
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
