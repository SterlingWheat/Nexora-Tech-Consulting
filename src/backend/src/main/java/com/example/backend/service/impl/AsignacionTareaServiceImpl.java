package com.example.backend.service.impl;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.AsignacionTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Voluntario;
import com.example.backend.notification.NotificationOutboxService;
import com.example.backend.repository.AsignacionTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.AsignacionTareaService;
import com.example.backend.service.HistorialTareaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AsignacionTareaServiceImpl implements AsignacionTareaService {

    private final AsignacionTareaRepository asignacionTareaRepository;
    private final TareaRepository tareaRepository;
    private final VoluntarioRepository voluntarioRepository;
    private final HistorialTareaService historialTareaService;
    private final NotificationOutboxService notificationOutboxService;
    private static final DateTimeFormatter ASSIGNMENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AsignacionTareaServiceImpl(AsignacionTareaRepository asignacionTareaRepository,
                                      TareaRepository tareaRepository,
                                      VoluntarioRepository voluntarioRepository,
                                      HistorialTareaService historialTareaService,
                                      NotificationOutboxService notificationOutboxService) {
        this.asignacionTareaRepository = asignacionTareaRepository;
        this.tareaRepository = tareaRepository;
        this.voluntarioRepository = voluntarioRepository;
        this.historialTareaService = historialTareaService;
        this.notificationOutboxService = notificationOutboxService;
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
            if (!asignacionTareaRepository.existsActiveByTareaAndVoluntario(idTarea, idVoluntario)) {
                String nombreCompleto = (voluntario.getNombre() + " "
                        + voluntario.getApellido()).trim();
                if (voluntario.getEmail() == null || voluntario.getEmail().isBlank()
                        || nombreCompleto.isBlank()) {
                    throw new BusinessValidationException(
                            "El voluntario debe tener nombre y correo para recibir la notificación.");
                }
                AsignacionTarea asignacion = AsignacionTarea.builder()
                        .tarea(tarea)
                        .voluntario(voluntario)
                        .estadoRespuesta("Asignado")
                        .build();
                nuevasAsignaciones.add(asignacion);
                nombresVoluntarios.add(nombreCompleto);
            }
        }

        if (!nuevasAsignaciones.isEmpty()) {
            List<AsignacionTarea> asignacionesGuardadas = asignacionTareaRepository.saveAll(nuevasAsignaciones);
            asignacionesGuardadas.forEach(asignacion -> notificationOutboxService.enqueue(
                            "task-assignment:" + asignacion.getIdAsignacion(),
                            NotificationTemplateType.ASIGNACION,
                            asignacion.getVoluntario().getEmail(),
                            (asignacion.getVoluntario().getNombre() + " "
                                    + asignacion.getVoluntario().getApellido()).trim(),
                            tarea.getTitulo(),
                            assignmentDate(asignacion)));
            
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

    private String assignmentDate(AsignacionTarea assignment) {
        LocalDateTime assignedAt = assignment.getFechaAsignacion() == null
                ? LocalDateTime.now()
                : assignment.getFechaAsignacion();
        return assignedAt.format(ASSIGNMENT_DATE_FORMAT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionTarea> obtenerAsignacionesPorTarea(Integer idTarea) {
        return asignacionTareaRepository.findByTareaIdTarea(idTarea);
    }

    @Override
    @Transactional
    public void cancelarAsignacion(Integer idTarea, Integer idAsignacion, Integer idUsuarioGestor) {
        Tarea tarea = tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + idTarea));
        AsignacionTarea asignacion = asignacionTareaRepository.findById(idAsignacion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignación no encontrada con ID: " + idAsignacion));

        if (!idTarea.equals(asignacion.getTarea().getIdTarea())) {
            throw new ResourceNotFoundException("Asignación no encontrada para la tarea indicada.");
        }
        if ("Cancelada".equalsIgnoreCase(asignacion.getEstadoRespuesta())
                || "Rechazada".equalsIgnoreCase(asignacion.getEstadoRespuesta())) {
            throw new ConflictException("La asignación ya está cancelada o rechazada.");
        }

        Voluntario voluntario = asignacion.getVoluntario();
        String nombreCompleto = (voluntario.getNombre() + " " + voluntario.getApellido()).trim();
        if (voluntario.getEmail() == null || voluntario.getEmail().isBlank()
                || nombreCompleto.isBlank()) {
            throw new BusinessValidationException(
                    "El voluntario debe tener nombre y correo para recibir la notificación de cancelación.");
        }

        asignacion.setEstadoRespuesta("Cancelada");
        asignacionTareaRepository.save(asignacion);
        notificationOutboxService.enqueue(
                "task-assignment-cancellation:" + idAsignacion,
                NotificationTemplateType.CANCELACION_ASIGNACION,
                voluntario.getEmail(),
                nombreCompleto,
                tarea.getTitulo(),
                LocalDateTime.now().format(ASSIGNMENT_DATE_FORMAT));
        historialTareaService.registrarAccion(
                idTarea,
                idUsuarioGestor,
                "Se canceló la asignación de " + nombreCompleto + ".");

        if ("Asignada".equalsIgnoreCase(tarea.getEstado())
                && asignacionTareaRepository.countActiveByTareaId(idTarea) == 0) {
            tarea.setEstado("Pendiente");
            tareaRepository.save(tarea);
        }
    }
}