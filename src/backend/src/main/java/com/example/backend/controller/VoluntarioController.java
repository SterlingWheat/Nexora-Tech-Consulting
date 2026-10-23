package com.example.backend.controller;

import com.example.backend.dto.VoluntarioRequestDTO;
import com.example.backend.dto.VoluntarioResponseDTO;
import com.example.backend.model.Voluntario;
import com.example.backend.service.VoluntarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/voluntarios")
@CrossOrigin(origins = "*")
public class VoluntarioController {

    private final VoluntarioService voluntarioService;

    public VoluntarioController(VoluntarioService voluntarioService) {
        this.voluntarioService = voluntarioService;
    }

    @PostMapping
    public ResponseEntity<?> registrarVoluntario(@Valid @RequestBody VoluntarioRequestDTO request) {
        try {
            Voluntario guardado = voluntarioService.registrarVoluntario(mapearAEntidad(request));
            return new ResponseEntity<>(mapearADTO(guardado), HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // Criterio 2: documento o correo duplicado
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarVoluntario(@PathVariable Integer id, @Valid @RequestBody VoluntarioRequestDTO request) {
        try {
            Voluntario actualizado = voluntarioService.actualizarVoluntario(id, mapearAEntidad(request));
            return ResponseEntity.ok(mapearADTO(actualizado));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<VoluntarioResponseDTO>> listarVoluntarios() {
        List<VoluntarioResponseDTO> voluntarios = voluntarioService.listarVoluntarios().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(voluntarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerVoluntario(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(mapearADTO(voluntarioService.obtenerVoluntarioPorId(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Criterio 4: validaciones de la API con mensaje claro por campo
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> manejarValidacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(mensaje);
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
