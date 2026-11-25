package com.example.backend.controller;

import com.example.backend.dto.AsignarHabilidadesRequestDTO;
import com.example.backend.dto.HabilidadDTO;
import com.example.backend.model.Habilidad;
import com.example.backend.service.HabilidadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// RF-23: habilidades asignadas a un voluntario, usadas para filtrar el calendario de disponibilidad
@RestController
@RequestMapping("/api/voluntarios/{idVoluntario}/habilidades")
@CrossOrigin(origins = "*")
public class VoluntarioHabilidadController {

    private final HabilidadService habilidadService;

    public VoluntarioHabilidadController(HabilidadService habilidadService) {
        this.habilidadService = habilidadService;
    }

    @GetMapping
    public ResponseEntity<List<HabilidadDTO>> obtener(@PathVariable Integer idVoluntario) {
        return ResponseEntity.ok(aDTOs(habilidadService.obtenerHabilidadesDeVoluntario(idVoluntario)));
    }

    @PutMapping
    public ResponseEntity<List<HabilidadDTO>> reemplazar(@PathVariable Integer idVoluntario,
                                                          @Valid @RequestBody AsignarHabilidadesRequestDTO request) {
        return ResponseEntity.ok(aDTOs(habilidadService.asignarHabilidades(idVoluntario, request.getIdsHabilidades())));
    }

    private List<HabilidadDTO> aDTOs(List<Habilidad> habilidades) {
        return habilidades.stream()
                .map(h -> new HabilidadDTO(h.getIdHabilidad(), h.getNombre()))
                .collect(Collectors.toList());
    }
}
