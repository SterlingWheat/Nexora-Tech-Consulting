package com.example.backend.controller;

import com.example.backend.dto.TareaRequestDTO;
import com.example.backend.dto.TareaResponseDTO;
import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import com.example.backend.service.TareaService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tareas")
@CrossOrigin(origins = "*")
public class TareaController {

    private final TareaService tareaService;

    //@Autowired
    public TareaController(TareaService tareaService) {
        this.tareaService = tareaService;
    }

    @PostMapping
    public ResponseEntity<TareaResponseDTO> crearTarea(@Valid @RequestBody TareaRequestDTO request) {
        Tarea nuevaTarea = new Tarea();
        nuevaTarea.setTitulo(request.getTitulo());
        nuevaTarea.setDescripcion(request.getDescripcion());
        
        CategoriaTarea categoria = new CategoriaTarea();
        categoria.setIdCategoria(request.getIdCategoria());
        nuevaTarea.setCategoria(categoria);

        Tarea tareaGuardada = tareaService.crearTarea(nuevaTarea);
        return new ResponseEntity<>(mapearADTO(tareaGuardada), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarTarea(@PathVariable Integer id, @Valid @RequestBody TareaRequestDTO request) {
        try {
            Tarea detallesActualizados = new Tarea();
            detallesActualizados.setTitulo(request.getTitulo());
            detallesActualizados.setDescripcion(request.getDescripcion());
            
            CategoriaTarea categoria = new CategoriaTarea();
            categoria.setIdCategoria(request.getIdCategoria());
            detallesActualizados.setCategoria(categoria);

            Tarea tareaEditada = tareaService.editarTarea(id, detallesActualizados);
            return ResponseEntity.ok(mapearADTO(tareaEditada));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<TareaResponseDTO>> listarTareas() {
        List<TareaResponseDTO> tareas = tareaService.listarTareas().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tareas);
    }

    // Criterio 4: Catálogo de categorías
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaTarea>> listarCategorias() {
        List<CategoriaTarea> categorias = tareaService.listarCategorias();
        return ResponseEntity.ok(categorias);
    }

    // Método auxiliar para mapear de Entidad a DTO
    private TareaResponseDTO mapearADTO(Tarea tarea) {
        TareaResponseDTO dto = new TareaResponseDTO();
        dto.setIdTarea(tarea.getIdTarea());
        dto.setTitulo(tarea.getTitulo());
        dto.setDescripcion(tarea.getDescripcion());
        if (tarea.getCategoria() != null) {
            dto.setIdCategoria(tarea.getCategoria().getIdCategoria());
            dto.setNombreCategoria(tarea.getCategoria().getNombre());
        }
        dto.setEstado(tarea.getEstado());
        dto.setAvance(tarea.getAvance());
        dto.setFechaCreacion(tarea.getFechaCreacion());
        return dto;
    }
}