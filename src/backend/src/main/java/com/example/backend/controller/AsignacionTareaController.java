package com.example.backend.controller;

import com.example.backend.dto.AsignacionRequestDTO;
import com.example.backend.dto.AsignacionResponseDTO;
import com.example.backend.model.AsignacionTarea;
import com.example.backend.model.Usuario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.AsignacionTareaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tareas/{idTarea}/asignaciones")
@CrossOrigin(origins = "*")
public class AsignacionTareaController {

    private final AsignacionTareaService asignacionTareaService;
    private final UsuarioRepository usuarioRepository;

    public AsignacionTareaController(AsignacionTareaService asignacionTareaService, UsuarioRepository usuarioRepository) {
        this.asignacionTareaService = asignacionTareaService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public ResponseEntity<List<AsignacionResponseDTO>> asignarVoluntarios(
            @PathVariable Integer idTarea,
            @Valid @RequestBody AsignacionRequestDTO request,
            Authentication authentication) {
        
        // Obtener el usuario autenticado que realiza la asignación (HU2 Criterio 3)
        String email = authentication.getName();
        Usuario usuarioAsignador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado en el sistema"));

        List<AsignacionTarea> asignaciones = asignacionTareaService.asignarVoluntarios(
                idTarea, 
                request.getIdVoluntarios(), 
                usuarioAsignador.getIdUsuario()
        );

        List<AsignacionResponseDTO> response = asignaciones.stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AsignacionResponseDTO>> listarAsignaciones(@PathVariable Integer idTarea) {
        List<AsignacionResponseDTO> response = asignacionTareaService.obtenerAsignacionesPorTarea(idTarea).stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private AsignacionResponseDTO mapearADTO(AsignacionTarea asignacion) {
        AsignacionResponseDTO dto = new AsignacionResponseDTO();
        dto.setIdAsignacion(asignacion.getIdAsignacion());
        dto.setIdTarea(asignacion.getTarea().getIdTarea());
        dto.setIdVoluntario(asignacion.getVoluntario().getIdVoluntario());
        
        String nombreCompleto = asignacion.getVoluntario().getNombre() + " " + asignacion.getVoluntario().getApellido();
        dto.setNombreVoluntario(nombreCompleto.trim());
        
        dto.setEstadoRespuesta(asignacion.getEstadoRespuesta());
        dto.setFechaAsignacion(asignacion.getFechaAsignacion());
        return dto;
    }
}