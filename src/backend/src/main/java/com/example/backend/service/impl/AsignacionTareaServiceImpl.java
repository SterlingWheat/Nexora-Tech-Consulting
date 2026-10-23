package com.example.backend.service.impl;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.AsignacionTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.AsignacionTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.AsignacionTareaService;
import com.example.backend.service.HistorialTareaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AsignacionTareaServiceImpl implements AsignacionTareaService {

    private final AsignacionTareaRepository asignacionTareaRepository;
    private final TareaRepository tareaRepository;
    private final VoluntarioRepository voluntarioRepository;
    private final HistorialTareaService historialTareaService;

    public AsignacionTareaServiceImpl(AsignacionTareaRepository asignacionTareaRepository,
                                      TareaRepository tareaRepository,
                                      VoluntarioRepository voluntarioRepository,
                                      HistorialTareaService historialTareaService) {
        this.asignacionTareaRepository = asignacionTareaRepository;
        this.tareaRepository = tareaRepository;
        this.voluntarioRepository = voluntarioRepository;
        this.historialTareaService = historialTareaService;
    }

    @Override
    @Transactional
    public List<AsignacionTarea> asignarVoluntarios(Integer idTarea, List<Integer> idVoluntarios, Integer idUsuarioAsignador) {
        Tarea tarea = tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + idTarea));

        if ("Finalizada".equalsIgnoreCase(tarea.getEstado()) || "Cancelada".equalsIgnoreCase(tarea.getEstado())) {
            throw new BusinessValidationException("No se pueden asignar voluntarios a una tarea Finalizada o Cancelada.");
        }

        List<AsignacionTarea> nuevasAsignaciones = new ArrayList<>();
        List<String> nombresVoluntarios = new ArrayList<>();

        for (Integer idVoluntario : idVoluntarios) {
            Voluntario voluntario = voluntarioRepository.findById(idVoluntario)
                    .orElseThrow(() -> new ResourceNotFoundException("Voluntario no encontrado con ID: " + idVoluntario));

            // HU2 Criterio 2: Validar que el voluntario esté activo
            if (!Boolean.TRUE.equals(voluntario.getActivo())) {
                throw new BusinessValidationException("El voluntario " + voluntario.getNombre() + " no está activo y no puede ser asignado.");
            }

            // Evitar duplicidades
            if (!asignacionTareaRepository.existsByTareaIdTareaAndVoluntarioIdVoluntario(idTarea, idVoluntario)) {
                AsignacionTarea asignacion = AsignacionTarea.builder()
                        .tarea(tarea)
                        .voluntario(voluntario)
                        .estadoRespuesta("Asignado")
                        .build();
                nuevasAsignaciones.add(asignacion);
                nombresVoluntarios.add(voluntario.getNombre() + " " + voluntario.getApellido());
            }
        }

        if (!nuevasAsignaciones.isEmpty()) {
            asignacionTareaRepository.saveAll(nuevasAsignaciones);
            
            // HU2 Criterio 1: La tarea pasa a Asignada si estaba Pendiente
            if ("Pendiente".equalsIgnoreCase(tarea.getEstado())) {
                tarea.setEstado("Asignada");
                tareaRepository.save(tarea);
            }

            // HU2 Criterio 3: Registrar quién asignó y cuándo (el cuándo se setea en el PrePersist de Historial)
            String detalle = "Se asignaron los voluntarios: " + String.join(", ", nombresVoluntarios);
            historialTareaService.registrarAccion(idTarea, idUsuarioAsignador, detalle);
        }

        return nuevasAsignaciones;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionTarea> obtenerAsignacionesPorTarea(Integer idTarea) {
        return asignacionTareaRepository.findByTareaIdTarea(idTarea);
    }
}