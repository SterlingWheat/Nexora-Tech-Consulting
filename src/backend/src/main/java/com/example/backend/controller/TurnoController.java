package com.example.backend.controller;

import com.example.backend.dto.TurnoRequestDTO;
import com.example.backend.dto.TurnoResponseDTO;
import com.example.backend.model.Turno;
import com.example.backend.model.Usuario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.TurnoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/turnos")
@CrossOrigin(origins = "*")
public class TurnoController {

    private final TurnoService turnoService;
    private final UsuarioRepository usuarioRepository;

    public TurnoController(TurnoService turnoService, UsuarioRepository usuarioRepository) {
        this.turnoService = turnoService;
        this.usuarioRepository = usuarioRepository;
    }

    private Integer obtenerIdGestor(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Usuario no autenticado.");
        }
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado en el sistema"));
        return usuario.getIdUsuario();
    }

    @PostMapping
    public ResponseEntity<TurnoResponseDTO> crearTurno(
            @Valid @RequestBody TurnoRequestDTO request, 
            Authentication authentication) {
        
        Turno nuevo = turnoService.crearTurno(aEntidad(request), obtenerIdGestor(authentication));
        return new ResponseEntity<>(aDTO(nuevo), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TurnoResponseDTO> actualizarTurno(
            @PathVariable Integer id,
            @Valid @RequestBody TurnoRequestDTO request,
            Authentication authentication) {
            
        Turno actualizado = turnoService.actualizarTurno(id, aEntidad(request), obtenerIdGestor(authentication));
        return ResponseEntity.ok(aDTO(actualizado));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<TurnoResponseDTO> cancelarTurno(
            @PathVariable Integer id, 
            Authentication authentication) {
            
        return ResponseEntity.ok(aDTO(turnoService.cancelarTurno(id, obtenerIdGestor(authentication))));
    }

    @GetMapping
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnos() {
        return ResponseEntity.ok(turnoService.listarTurnos().stream().map(this::aDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TurnoResponseDTO> obtenerTurno(@PathVariable Integer id) {
        return ResponseEntity.ok(aDTO(turnoService.obtenerTurnoPorId(id)));
    }

    private TurnoResponseDTO aDTO(Turno t) {
        TurnoResponseDTO dto = new TurnoResponseDTO();
        dto.setIdTurno(t.getIdTurno());
        dto.setFecha(t.getFecha());
        dto.setHoraInicio(t.getHoraInicio());
        dto.setHoraFin(t.getHoraFin());
        dto.setActividad(t.getActividad());
        dto.setEstado(t.getEstado());
        dto.setCuposTotales(t.getCuposTotales());
        dto.setCuposOcupados(t.getCuposOcupados());
        dto.setCuposLibres(t.getCuposLibres());
        dto.setFechaCreacion(t.getFechaCreacion());
        return dto;
    }

    private Turno aEntidad(TurnoRequestDTO request) {
        return Turno.builder()
                .fecha(request.getFecha())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .actividad(request.getActividad())
                .cuposTotales(request.getCuposTotales())
                .build();
    }
}