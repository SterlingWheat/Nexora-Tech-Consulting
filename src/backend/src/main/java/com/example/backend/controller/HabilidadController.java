package com.example.backend.controller;

import com.example.backend.dto.HabilidadDTO;
import com.example.backend.service.HabilidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/habilidades")
@RequiredArgsConstructor
// Solo los administradores o gestores deben poder manipular el catálogo maestro de habilidades.
@PreAuthorize("hasRole('ADMIN')") 
public class HabilidadController {

    private final HabilidadService habilidadService;

    @PostMapping
    public ResponseEntity<HabilidadDTO> createHabilidad(@Valid @RequestBody HabilidadDTO habilidadDTO) {
        HabilidadDTO created = habilidadService.createHabilidad(habilidadDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabilidadDTO> updateHabilidad(
            @PathVariable Long id, 
            @Valid @RequestBody HabilidadDTO habilidadDTO) {
        HabilidadDTO updated = habilidadService.updateHabilidad(id, habilidadDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabilidad(@PathVariable Long id) {
        habilidadService.deleteHabilidad(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_TAREAS')") // Lectura más permisiva si se requiere
    public ResponseEntity<HabilidadDTO> getHabilidadById(@PathVariable Long id) {
        return ResponseEntity.ok(habilidadService.getHabilidadById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR_TAREAS')")
    public ResponseEntity<List<HabilidadDTO>> getAllHabilidades() {
        return ResponseEntity.ok(habilidadService.getAllHabilidades());
    }
}