package com.example.backend.controller;

import com.example.backend.dto.DisponibilidadRequestDTO;
import com.example.backend.dto.DisponibilidadResponseDTO;
import com.example.backend.dto.FranjaDTO;
import com.example.backend.model.Disponibilidad;
import com.example.backend.service.DisponibilidadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/voluntarios/{idVoluntario}/disponibilidad")
@CrossOrigin(origins = "*")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping
    public ResponseEntity<DisponibilidadResponseDTO> obtener(@PathVariable Integer idVoluntario) {
        List<Disponibilidad> franjas = disponibilidadService.obtenerDisponibilidad(idVoluntario);
        return ResponseEntity.ok(aDTO(idVoluntario, franjas, List.of()));
    }

    @PutMapping
    public ResponseEntity<DisponibilidadResponseDTO> reemplazar(@PathVariable Integer idVoluntario,
                                                                @Valid @RequestBody DisponibilidadRequestDTO request) {
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
