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
        return tareaRepository.filtrarTareas(estado, idCategoria, idResponsable, fechaInicio, fechaFin, pageable);
    }

    @Override
    @Transactional
    public Tarea cambiarEstado(Integer idTarea, String nuevoEstado, Integer idUsuario) {
        Tarea tarea = obtenerTareaPorId(idTarea);
        String estadoActual = tarea.getEstado();

        List<String> estadosValidos = Arrays.asList("Pendiente", "Asignada", "En progreso", "Finalizada", "Cancelada");
        if (!estadosValidos.contains(nuevoEstado)) {
            throw new BusinessValidationException("El estado '" + nuevoEstado + "' no es un estado controlado válido.");
        }

        if ("Finalizada".equalsIgnoreCase(estadoActual) || "Cancelada".equalsIgnoreCase(estadoActual)) {
            throw new BusinessValidationException("Transición no permitida: La tarea ya se encuentra " + estadoActual + ".");
        }

        tarea.setEstado(nuevoEstado);
        Tarea tareaGuardada = tareaRepository.save(tarea);

        // HU2 Criterio 1: Registrar en el historial el cambio de estado con tipoCambio "ESTADO"
        String detalle = "El estado cambió de '" + estadoActual + "' a '" + nuevoEstado + "'.";
        historialTareaService.registrarAccionDetallada(idTarea, idUsuario, "ESTADO", detalle, estadoActual, nuevoEstado);

        return tareaGuardada;
    }

    @Override
    @Transactional
    public Tarea registrarAvance(Integer idTarea, RegistroAvanceRequestDTO request, Integer idUsuario) {
        Tarea tarea = obtenerTareaPorId(idTarea);

        if (!"En progreso".equalsIgnoreCase(tarea.getEstado())) {
            throw new BusinessValidationException("Solo se puede registrar avance en tareas que están 'En progreso'.");
        }

        Short avanceAnterior = tarea.getAvance();
        Short avanceNuevo = request.getAvance();
        tarea.setAvance(avanceNuevo);
        Tarea tareaGuardada = tareaRepository.save(tarea);

        StringBuilder detalleBuilder = new StringBuilder();
        detalleBuilder.append("Se actualizó el avance de ").append(avanceAnterior != null ? avanceAnterior : 0)
                      .append("% a ").append(avanceNuevo).append("%.");

        if (request.getObservacion() != null && !request.getObservacion().trim().isEmpty()) {
            detalleBuilder.append(" Observación: ").append(request.getObservacion().trim());
        }

        // HU2 Criterio 1: Registrar en el historial el avance operativo como "EDICION"
        historialTareaService.registrarAccionDetallada(
            idTarea, 
            idUsuario, 
            "EDICION", 
            detalleBuilder.toString(), 
            String.valueOf(avanceAnterior != null ? avanceAnterior : 0), 
            String.valueOf(avanceNuevo)
        );

        return tareaGuardada;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaTarea> listarCategorias() {
        return categoriaTareaRepository.findAll();
    }
}