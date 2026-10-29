package com.example.backend.service.impl;

import com.example.backend.dto.RegistroAvanceRequestDTO;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import com.example.backend.repository.CategoriaTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.service.HistorialTareaService;
import com.example.backend.service.TareaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class TareaServiceImpl implements TareaService {

    private final TareaRepository tareaRepository;
    private final CategoriaTareaRepository categoriaTareaRepository;
    private final HistorialTareaService historialTareaService;

    public TareaServiceImpl(TareaRepository tareaRepository, 
                            CategoriaTareaRepository categoriaTareaRepository,
                            HistorialTareaService historialTareaService) {
        this.tareaRepository = tareaRepository;
        this.categoriaTareaRepository = categoriaTareaRepository;
        this.historialTareaService = historialTareaService;
    }

    @Override
    @Transactional
    public Tarea crearTarea(Tarea tarea) {
        // HU1 y Tarea: La tarea se crea en estado Pendiente
        tarea.setEstado("Pendiente");
        tarea.setAvance((short) 0);
        
        CategoriaTarea categoria = categoriaTareaRepository.findById(tarea.getCategoria().getIdCategoria())
                .orElseThrow(() -> new ResourceNotFoundException("La categoría seleccionada no existe."));
        
        tarea.setCategoria(categoria);
        return tareaRepository.save(tarea);
    }

    @Override
    @Transactional
    public Tarea editarTarea(Integer idTarea, Tarea tareaDetalles) {
        Tarea tareaExistente = tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + idTarea));

        if ("Finalizada".equalsIgnoreCase(tareaExistente.getEstado()) || 
            "Cancelada".equalsIgnoreCase(tareaExistente.getEstado())) {
            throw new BusinessValidationException("No se puede editar la tarea porque se encuentra " + tareaExistente.getEstado());
        }

        tareaExistente.setTitulo(tareaDetalles.getTitulo());
        tareaExistente.setDescripcion(tareaDetalles.getDescripcion());
        
        if (tareaDetalles.getCategoria() != null && tareaDetalles.getCategoria().getIdCategoria() != null) {
            CategoriaTarea categoria = categoriaTareaRepository.findById(tareaDetalles.getCategoria().getIdCategoria())
                    .orElseThrow(() -> new ResourceNotFoundException("La categoría seleccionada no existe."));
            tareaExistente.setCategoria(categoria);
        }

        return tareaRepository.save(tareaExistente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tarea> listarTareas() {
        return tareaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Tarea obtenerTareaPorId(Integer idTarea) {
        return tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + idTarea));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Tarea> filtrarTareas(String estado, Integer idCategoria, Integer idResponsable,
                                     LocalDateTime fechaInicio, LocalDateTime fechaFin, Pageable pageable) {
        // HU1 Criterio 1 y 2: Filtrado en la base de datos
        return tareaRepository.filtrarTareas(estado, idCategoria, idResponsable, fechaInicio, fechaFin, pageable);
    }

    @Override
    @Transactional
    public Tarea cambiarEstado(Integer idTarea, String nuevoEstado, Integer idUsuario) {
        Tarea tarea = obtenerTareaPorId(idTarea);
        String estadoActual = tarea.getEstado();

        // HU3 Criterio 1: Estados permitidos
        List<String> estadosValidos = Arrays.asList("Pendiente", "Asignada", "En progreso", "Finalizada", "Cancelada");
        if (!estadosValidos.contains(nuevoEstado)) {
            throw new BusinessValidationException("El estado '" + nuevoEstado + "' no es un estado controlado válido.");
        }

        // HU3 Criterio 3: Transiciones no permitidas (Evitar mover desde estados terminales)
        if ("Finalizada".equalsIgnoreCase(estadoActual) || "Cancelada".equalsIgnoreCase(estadoActual)) {
            throw new BusinessValidationException("Transición no permitida: La tarea ya se encuentra " + estadoActual + ".");
        }

        tarea.setEstado(nuevoEstado);
        Tarea tareaGuardada = tareaRepository.save(tarea);

        // HU3 Criterio 2: Registrar en el historial la fecha, usuario y cambio de estado
        String detalle = "El estado cambió de '" + estadoActual + "' a '" + nuevoEstado + "'.";
        historialTareaService.registrarAccion(idTarea, idUsuario, detalle);

        return tareaGuardada;
    }

    @Override
    @Transactional
    public Tarea registrarAvance(Integer idTarea, RegistroAvanceRequestDTO request, Integer idUsuario) {
        Tarea tarea = obtenerTareaPorId(idTarea);

        // HU Criterio 1: Solo tareas "En progreso"
        if (!"En progreso".equalsIgnoreCase(tarea.getEstado())) {
            throw new BusinessValidationException("Solo se puede registrar avance en tareas que están 'En progreso'.");
        }

        Short avanceAnterior = tarea.getAvance();
        tarea.setAvance(request.getAvance());
        Tarea tareaGuardada = tareaRepository.save(tarea);

        // Crear detalle para el historial detallando el cambio de avance y la observación
        StringBuilder detalleBuilder = new StringBuilder();
        detalleBuilder.append("Se actualizó el avance de ").append(avanceAnterior != null ? avanceAnterior : 0)
                      .append("% a ").append(request.getAvance()).append("%.");

        // HU Criterio 2: Registrar observación con autor (el autor ya lo maneja HistorialTareaService)
        if (request.getObservacion() != null && !request.getObservacion().trim().isEmpty()) {
            detalleBuilder.append(" Observación: ").append(request.getObservacion().trim());
        }

        historialTareaService.registrarAccion(idTarea, idUsuario, detalleBuilder.toString());

        return tareaGuardada;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaTarea> listarCategorias() {
        return categoriaTareaRepository.findAll();
    }
}