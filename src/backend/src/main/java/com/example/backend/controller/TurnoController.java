package com.example.backend.controller;

import com.example.backend.dto.TurnoRequestDTO;
import com.example.backend.dto.TurnoResponseDTO;
import com.example.backend.model.Turno;
import com.example.backend.service.TurnoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/turnos")
@CrossOrigin(origins = "*")
public class TurnoController {

    private final TurnoService turnoService;

    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @PostMapping
    public ResponseEntity<TurnoResponseDTO> crearTurno(@Valid @RequestBody TurnoRequestDTO request) {
        Turno turno = Turno.builder()
                .fecha(request.getFecha())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .actividad(request.getActividad())
                .cuposTotales(request.getCuposTotales())
                .build();
        return new ResponseEntity<>(aDTO(turnoService.crearTurno(turno)), HttpStatus.CREATED);
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
        dto.setCuposTotales(t.getCuposTotales());
        dto.setCuposOcupados(t.getCuposOcupados());
        dto.setCuposLibres(t.getCuposLibres());
        dto.setFechaCreacion(t.getFechaCreacion());
        return dto;
    }
}
