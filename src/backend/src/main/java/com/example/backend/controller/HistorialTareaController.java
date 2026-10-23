package com.example.backend.controller;

import com.example.backend.dto.HistorialTareaResponseDTO;
import com.example.backend.model.HistorialTarea;
import com.example.backend.service.HistorialTareaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tareas/{idTarea}/historial")
@CrossOrigin(origins = "*")
public class HistorialTareaController {

    private final HistorialTareaService historialTareaService;

    public HistorialTareaController(HistorialTareaService historialTareaService) {
        this.historialTareaService = historialTareaService;
    }

    @GetMapping
    public ResponseEntity<List<HistorialTareaResponseDTO>> listarHistorial(@PathVariable Integer idTarea) {
        List<HistorialTareaResponseDTO> response = historialTareaService.obtenerHistorialPorTarea(idTarea).stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private HistorialTareaResponseDTO mapearADTO(HistorialTarea historial) {
        HistorialTareaResponseDTO dto = new HistorialTareaResponseDTO();
        dto.setIdHistorial(historial.getIdHistorial());
        dto.setIdTarea(historial.getTarea().getIdTarea());
        dto.setIdUsuario(historial.getUsuario().getIdUsuario());
        dto.setNombreUsuario(historial.getUsuario().getNombre());
        dto.setDetalle(historial.getDetalle());
        dto.setFechaRegistro(historial.getFechaRegistro());
        return dto;
    }
}