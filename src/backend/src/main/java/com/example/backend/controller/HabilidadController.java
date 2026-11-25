package com.example.backend.controller;

import com.example.backend.dto.HabilidadDTO;
import com.example.backend.dto.HabilidadRequestDTO;
import com.example.backend.model.Habilidad;
import com.example.backend.service.HabilidadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// RF-23: catálogo de habilidades, usado para etiquetar voluntarios y filtrar el calendario de disponibilidad
@RestController
@RequestMapping("/api/habilidades")
@CrossOrigin(origins = "*")
public class HabilidadController {

    private final HabilidadService habilidadService;

    public HabilidadController(HabilidadService habilidadService) {
        this.habilidadService = habilidadService;
    }

    @GetMapping
    public ResponseEntity<List<HabilidadDTO>> listar() {
        return ResponseEntity.ok(habilidadService.listarHabilidades().stream()
                .map(this::aDTO)
                .collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<HabilidadDTO> crear(@Valid @RequestBody HabilidadRequestDTO request) {
        Habilidad creada = habilidadService.crearHabilidad(request.getNombre());
        return new ResponseEntity<>(aDTO(creada), HttpStatus.CREATED);
    }

    private HabilidadDTO aDTO(Habilidad h) {
        return new HabilidadDTO(h.getIdHabilidad(), h.getNombre());
    }
}
