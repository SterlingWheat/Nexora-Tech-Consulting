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

            // HU2 Criterio 2: Validar que el voluntario est  activo
            if (!Boolean.TRUE.equals(voluntario.getActivo())) {
                throw new BusinessValidationException("El voluntario " + voluntario.getNombre() + " no est  activo y no puede ser asignado.");
            }

            // Evitar duplicidades
            if (!asignacionTareaRepository.existsActiveByTareaAndVoluntario(idTarea, idVoluntario)) {
                String nombreCompleto = (voluntario.getNombre() + " " + voluntario.getApellido()).trim();
                if (voluntario.getEmail() == null || voluntario.getEmail().isBlank() || nombreCompleto.isBlank()) {
                    throw new BusinessValidationException(
                            "El voluntario debe tener nombre y correo para recibir la notificaci n.");
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

            // HU2 Criterio 3: Registrar qui n asign  y cu ndo
            String detalle = "Se asignaron los voluntarios: " + String.join(", ", nombresVoluntarios);
            historialTareaService.registrarAccion(idTarea, idUsuarioAsignador, detalle);
        }

        return nuevasAsignaciones;
    }

    @Override
    @Transactional
    public AsignacionTarea reemplazarVoluntario(Integer idTarea, Integer idAsignacionActual, Integer idNuevoVoluntario, Integer idUsuarioGestor) {
        Tarea tarea = tareaRepository.findById(idTarea)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada con ID: " + idTarea));

        if ("Finalizada".equalsIgnoreCase(tarea.getEstado()) || "Cancelada".equalsIgnoreCase(tarea.getEstado())) {
            throw new BusinessValidationException("No se pueden reemplazar voluntarios en una tarea Finalizada o Cancelada.");
        }

        AsignacionTarea asignacionAnterior = asignacionTareaRepository.findById(idAsignacionActual)
                .orElseThrow(() -> new ResourceNotFoundException("Asignaci n no encontrada con ID: " + idAsignacionActual));

        if (!idTarea.equals(asignacionAnterior.getTarea().getIdTarea())) {
            throw new ResourceNotFoundException("Asignaci n no encontrada para la tarea indicada.");
        }

        if ("Cancelada".equalsIgnoreCase(asignacionAnterior.getEstadoRespuesta())
                || "Rechazada".equalsIgnoreCase(asignacionAnterior.getEstadoRespuesta())) {
            throw new ConflictException("La asignaci n anterior ya est  cancelada o rechazada.");
        }

        Voluntario nuevoVoluntario = voluntarioRepository.findById(idNuevoVoluntario)
                .orElseThrow(() -> new ResourceNotFoundException("Voluntario no encontrado con ID: " + idNuevoVoluntario));

        if (!Boolean.TRUE.equals(nuevoVoluntario.getActivo())) {
            throw new BusinessValidationException("El voluntario " + nuevoVoluntario.getNombre() + " no est  activo y no puede ser asignado.");
        }

        if (asignacionAnterior.getVoluntario().getIdVoluntario().equals(idNuevoVoluntario)) {
            throw new BusinessValidationException("El nuevo voluntario seleccionado es el mismo que ya est  asignado.");
        }

        if (asignacionTareaRepository.existsActiveByTareaAndVoluntario(idTarea, idNuevoVoluntario)) {
            throw new BusinessValidationException("El nuevo voluntario ya se encuentra asignado activamente a esta tarea.");
        }

        Voluntario voluntarioAnterior = asignacionAnterior.getVoluntario();
        String nombreCompletoAnterior = (voluntarioAnterior.getNombre() + " " + voluntarioAnterior.getApellido()).trim();
        String nombreCompletoNuevo = (nuevoVoluntario.getNombre() + " " + nuevoVoluntario.getApellido()).trim();

        if (nuevoVoluntario.getEmail() == null || nuevoVoluntario.getEmail().isBlank()) {
            throw new BusinessValidationException("El nuevo voluntario debe tener un correo para recibir la notificaci n.");
        }

        // 1. Desactivar asignaci n previa pero mantenerla para el historial (Criterio 1)
        asignacionAnterior.setEstadoRespuesta("Cancelada");
        asignacionTareaRepository.save(asignacionAnterior);

        // 2. Crear nueva asignaci n
        AsignacionTarea nuevaAsignacion = AsignacionTarea.builder()
                .tarea(tarea)
                .voluntario(nuevoVoluntario)
                .estadoRespuesta("Asignado")
                .build();

        AsignacionTarea asignacionGuardada = asignacionTareaRepository.save(nuevaAsignacion);

        // 3. Notificar a voluntarios afectados (Criterio 2)
        // Notificaci n al voluntario retirado
        if (voluntarioAnterior.getEmail() != null && !voluntarioAnterior.getEmail().isBlank()) {
            notificationOutboxService.enqueue(
                    "task-assignment-replacement-out:" + idAsignacionActual + ":" + System.currentTimeMillis(),
                    NotificationTemplateType.CANCELACION_ASIGNACION,
                    voluntarioAnterior.getEmail(),
                    nombreCompletoAnterior,
                    tarea.getTitulo(),
                    LocalDateTime.now().format(ASSIGNMENT_DATE_FORMAT));
        }

        // Notificaci n al nuevo voluntario
        notificationOutboxService.enqueue(
                "task-assignment-replacement-in:" + asignacionGuardada.getIdAsignacion(),
                NotificationTemplateType.ASIGNACION,
                nuevoVoluntario.getEmail(),
                nombreCompletoNuevo,
                tarea.getTitulo(),
                assignmentDate(asignacionGuardada));

        // 4. Registro en el historial
        String detalle = "Se sustituy  al voluntario " + nombreCompletoAnterior + " por " + nombreCompletoNuevo + " ante un imprevisto.";
        historialTareaService.registrarAccion(idTarea, idUsuarioGestor, detalle);

        return asignacionGuardada;
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
                        "Asignaci n no encontrada con ID: " + idAsignacion));

        if (!idTarea.equals(asignacion.getTarea().getIdTarea())) {
            throw new ResourceNotFoundException("Asignaci n no encontrada para la tarea indicada.");
        }

        if ("Cancelada".equalsIgnoreCase(asignacion.getEstadoRespuesta())
                || "Rechazada".equalsIgnoreCase(asignacion.getEstadoRespuesta())) {
            throw new ConflictException("La asignaci n ya est  cancelada o rechazada.");
        }

        Voluntario voluntario = asignacion.getVoluntario();
        String nombreCompleto = (voluntario.getNombre() + " " + voluntario.getApellido()).trim();

        if (voluntario.getEmail() == null || voluntario.getEmail().isBlank()
                || nombreCompleto.isBlank()) {
            throw new BusinessValidationException(
                    "El voluntario debe tener nombre y correo para recibir la notificaci n de cancelaci n.");
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
                "Se cancel  la asignaci n de " + nombreCompleto + ".");

        if ("Asignada".equalsIgnoreCase(tarea.getEstado())
                && asignacionTareaRepository.countActiveByTareaId(idTarea) == 0) {
            tarea.setEstado("Pendiente");
            tareaRepository.save(tarea);
        }
    }
}