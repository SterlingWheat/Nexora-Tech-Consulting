package com.example.backend.controller;

import com.example.backend.dto.AsignacionTurnoRequestDTO;
import com.example.backend.dto.AsignacionTurnoResponseDTO;
import com.example.backend.dto.AsignacionTurnoResultadoDTO;
import com.example.backend.model.TurnoAssignmentRecord;
import com.example.backend.service.AsignacionTurnoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turnos/{idTurno}/asignaciones")
@CrossOrigin(origins = "*")
public class AsignacionTurnoController {

    private final AsignacionTurnoService service;

    public AsignacionTurnoController(AsignacionTurnoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AsignacionTurnoResultadoDTO> asignar(
            @PathVariable Integer idTurno,
            @Valid @RequestBody AsignacionTurnoRequestDTO request) {

        AsignacionTurnoService.ResultadoAsignacionTurno resultado =
                service.asignarVoluntarios(idTurno, request.getIdVoluntarios());

        List<AsignacionTurnoResponseDTO> asignaciones = resultado.asignaciones().stream()
                .map(this::aDto)
                .toList();

        return new ResponseEntity<>(
                new AsignacionTurnoResultadoDTO(asignaciones, resultado.advertencias()),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AsignacionTurnoResponseDTO>> listar(@PathVariable Integer idTurno) {
        return ResponseEntity.ok(service.listarAsignaciones(idTurno).stream().map(this::aDto).toList());
    }

    // HU2 Criterio 1: Confirmar asignación
    @PatchMapping("/{idAsignacion}/confirmar")
    public ResponseEntity<Void> confirmar(
            @PathVariable Integer idTurno,
            @PathVariable Long idAsignacion) {
        service.confirmarAsignacion(idTurno, idAsignacion);
        return ResponseEntity.noContent().build();
    }

    // HU2 Criterios 2 y 3: Cancelar asignación
    @PostMapping("/{idAsignacion}/cancelar")
    public ResponseEntity<Void> cancelar(
            @PathVariable Integer idTurno,
            @PathVariable Long idAsignacion) {
        service.cancelarAsignacion(idTurno, idAsignacion);
        return ResponseEntity.noContent().build();
    }

    private AsignacionTurnoResponseDTO aDto(TurnoAssignmentRecord assignment) {
        AsignacionTurnoResponseDTO dto = new AsignacionTurnoResponseDTO();
        dto.setIdAsignacion(assignment.idAsignacion());
        dto.setIdTurno(assignment.idTurno());
        dto.setIdVoluntario(assignment.idVoluntario());
        dto.setNombreVoluntario(assignment.nombreVoluntario());
        dto.setEstado(assignment.estado());
        return dto;
    }
}