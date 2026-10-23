package com.example.backend.controller;

import com.example.backend.dto.DisponibilidadRequestDTO;
import com.example.backend.dto.DisponibilidadResponseDTO;
import com.example.backend.dto.FranjaDTO;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Disponibilidad;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.DisponibilidadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/voluntarios/{idVoluntario}/disponibilidad")
@CrossOrigin(origins = "*")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;
    private final VoluntarioRepository voluntarioRepository;

    public DisponibilidadController(DisponibilidadService disponibilidadService, VoluntarioRepository voluntarioRepository) {
        this.disponibilidadService = disponibilidadService;
        this.voluntarioRepository = voluntarioRepository;
    }

    // Solución Error 1: Control de acceso para que un voluntario no vea/edite datos de otros
    private void verificarPermiso(Integer idVoluntario, Authentication auth) {
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_VOLUNTARIO"))) {
            Voluntario v = voluntarioRepository.findById(idVoluntario)
                    .orElseThrow(() -> new ResourceNotFoundException("Voluntario no encontrado con ID: " + idVoluntario));
            
            // Si el correo del voluntario a modificar no coincide con el del usuario autenticado, bloquear
            if (v.getEmail() == null || !v.getEmail().equalsIgnoreCase(auth.getName())) {
                throw new AccessDeniedException("Acceso denegado: no puedes gestionar la disponibilidad de otro voluntario.");
            }
        }
    }

    @GetMapping
    public ResponseEntity<DisponibilidadResponseDTO> obtener(@PathVariable Integer idVoluntario, Authentication authentication) {
        verificarPermiso(idVoluntario, authentication);
        
        List<Disponibilidad> franjas = disponibilidadService.obtenerDisponibilidad(idVoluntario);
        return ResponseEntity.ok(aDTO(idVoluntario, franjas, List.of()));
    }

    @PutMapping
    public ResponseEntity<DisponibilidadResponseDTO> reemplazar(@PathVariable Integer idVoluntario,
                                                                @Valid @RequestBody DisponibilidadRequestDTO request,
                                                                Authentication authentication) {
        verificarPermiso(idVoluntario, authentication);
        
        List<Disponibilidad> entrantes = request.getFranjas().stream()
                .map(f -> Disponibilidad.builder()
                        .diaSemana(f.getDiaSemana()).horaInicio(f.getHoraInicio()).horaFin(f.getHoraFin()).build())
                .collect(Collectors.toList());

        DisponibilidadService.Resultado resultado = disponibilidadService.reemplazarDisponibilidad(idVoluntario, entrantes);
        return ResponseEntity.ok(aDTO(idVoluntario, resultado.franjas(), resultado.advertencias()));
    }

    private DisponibilidadResponseDTO aDTO(Integer idVoluntario, List<Disponibilidad> franjas, List<String> advertencias) {
        List<FranjaDTO> dtos = franjas.stream()
                .map(f -> new FranjaDTO(f.getDiaSemana(), f.getHoraInicio(), f.getHoraFin()))
                .collect(Collectors.toList());
        return new DisponibilidadResponseDTO(idVoluntario, dtos, advertencias);
    }
}