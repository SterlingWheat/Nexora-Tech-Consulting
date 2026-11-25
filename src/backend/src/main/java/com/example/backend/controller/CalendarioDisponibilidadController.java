package com.example.backend.controller;

import com.example.backend.dto.CalendarioDisponibilidadResponseDTO;
import com.example.backend.dto.CalendarioFranjaDTO;
import com.example.backend.service.DisponibilidadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

// RF-23: calendario de disponibilidad de los voluntarios, para planificar turnos visualmente
@RestController
@RequestMapping("/api/disponibilidad")
@CrossOrigin(origins = "*")
public class CalendarioDisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public CalendarioDisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    // Criterio 1: al seleccionar un rango (p. ej. una semana) se ven las franjas disponibles por voluntario
    // Criterio 2: idVoluntario e idHabilidad permiten filtrar por voluntario o por habilidad
    @GetMapping("/calendario")
    public ResponseEntity<CalendarioDisponibilidadResponseDTO> calendario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idVoluntario,
            @RequestParam(required = false) Integer idHabilidad) {

        List<DisponibilidadService.Ocurrencia> ocurrencias =
                disponibilidadService.obtenerCalendario(desde, hasta, idVoluntario, idHabilidad);

        List<CalendarioFranjaDTO> franjas = ocurrencias.stream()
                .map(o -> new CalendarioFranjaDTO(o.idVoluntario(), o.nombreVoluntario(), o.apellidoVoluntario(),
                        o.fecha(), o.diaSemana(), o.horaInicio(), o.horaFin()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new CalendarioDisponibilidadResponseDTO(desde, hasta, franjas));
    }
}
