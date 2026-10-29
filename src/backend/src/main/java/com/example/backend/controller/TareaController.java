package com.example.backend.controller;

import com.example.backend.dto.CambioEstadoRequestDTO;
import com.example.backend.dto.RegistroAvanceRequestDTO;
import com.example.backend.dto.TareaRequestDTO;
import com.example.backend.dto.TareaResponseDTO;
import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Usuario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.TareaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tareas")
@CrossOrigin(origins = "*")
public class TareaController {

    private final TareaService tareaService;
    private final UsuarioRepository usuarioRepository;

    public TareaController(TareaService tareaService, UsuarioRepository usuarioRepository) {
        this.tareaService = tareaService;
        this.usuarioRepository = usuarioRepository;
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
    public ResponseEntity<TareaResponseDTO> editarTarea(@PathVariable Integer id, @Valid @RequestBody TareaRequestDTO request) {
        Tarea detallesActualizados = new Tarea();
        detallesActualizados.setTitulo(request.getTitulo());
        detallesActualizados.setDescripcion(request.getDescripcion());
        
        CategoriaTarea categoria = new CategoriaTarea();
        categoria.setIdCategoria(request.getIdCategoria());
        detallesActualizados.setCategoria(categoria);
        
        Tarea tareaEditada = tareaService.editarTarea(id, detallesActualizados);
        return ResponseEntity.ok(mapearADTO(tareaEditada));
    }

    // Endpoint original que devuelve la lista completa
    @GetMapping
    public ResponseEntity<List<TareaResponseDTO>> listarTareas() {
        List<TareaResponseDTO> tareas = tareaService.listarTareas().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tareas);
    }

    // HU1: Nuevo endpoint con paginación, filtros y orden (Criterios 1 y 2)
    @GetMapping("/filtrar")
    public ResponseEntity<Page<TareaResponseDTO>> filtrarTareas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idCategoria,
            @RequestParam(required = false) Integer idResponsable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        // Paginación y ordenamiento
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Transformar LocalDate a LocalDateTime para abarcar todo el día
        LocalDateTime inicio = (fechaInicio != null) ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = (fechaFin != null) ? fechaFin.atTime(LocalTime.MAX) : null;

        Page<Tarea> tareasPaginadas = tareaService.filtrarTareas(estado, idCategoria, idResponsable, inicio, fin, pageable);
        
        return ResponseEntity.ok(tareasPaginadas.map(this::mapearADTO));
    }

    // HU3: Cambiar estado (Criterios 1, 2 y 3)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<TareaResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody CambioEstadoRequestDTO request,
            Authentication authentication) {
        
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado para registrar el historial."));
        
        Tarea tarea = tareaService.cambiarEstado(id, request.getNuevoEstado(), usuario.getIdUsuario());
        return ResponseEntity.ok(mapearADTO(tarea));
    }

    // HU: Registrar Avance y Observaciones
    @PatchMapping("/{id}/avance")
    public ResponseEntity<TareaResponseDTO> registrarAvance(
            @PathVariable Integer id,
            @Valid @RequestBody RegistroAvanceRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado para registrar el avance."));

        Tarea tarea = tareaService.registrarAvance(id, request, usuario.getIdUsuario());
        return ResponseEntity.ok(mapearADTO(tarea));
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaTarea>> listarCategorias() {
        List<CategoriaTarea> categorias = tareaService.listarCategorias();
        return ResponseEntity.ok(categorias);
    }

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