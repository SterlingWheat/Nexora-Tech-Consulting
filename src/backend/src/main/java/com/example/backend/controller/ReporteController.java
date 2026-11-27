package com.example.backend.controller;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RF-45/46 (KAN-56): reportes de voluntarios, tareas y turnos para el Administrador
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/{tipo}")
    public ReporteTablaDTO obtenerReporte(@PathVariable String tipo) {
        return switch (tipo) {
            case "voluntarios" -> reporteService.reporteVoluntarios();
            case "tareas" -> reporteService.reporteTareas();
            case "turnos" -> reporteService.reporteTurnos();
            default -> throw new IllegalArgumentException("Tipo de reporte no soportado: " + tipo);
        };
    }
}
