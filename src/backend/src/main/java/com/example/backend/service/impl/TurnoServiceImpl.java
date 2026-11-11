package com.example.backend.service.impl;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.Turno;
import com.example.backend.model.TurnoAssignmentRecord;
import com.example.backend.repository.AsignacionTurnoRepository;
import com.example.backend.repository.CupoOcupado;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.notification.NotificationOutboxService;
import com.example.backend.service.CruceTurnoService;
import com.example.backend.service.HistorialTurnoService;
import com.example.backend.service.TurnoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TurnoServiceImpl implements TurnoService {

    private static final DateTimeFormatter TURN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TurnoRepository turnoRepository;
    private final AsignacionTurnoRepository asignacionRepository;
    private final NotificationOutboxService outboxService;
    private final CruceTurnoService cruceTurnoService;
    private final HistorialTurnoService historialTurnoService;
    private final Clock clock;

    public TurnoServiceImpl(
            TurnoRepository turnoRepository,
            AsignacionTurnoRepository asignacionRepository,
            NotificationOutboxService outboxService,
            CruceTurnoService cruceTurnoService,
            HistorialTurnoService historialTurnoService,
            Clock clock) {
        this.turnoRepository = turnoRepository;
        this.asignacionRepository = asignacionRepository;
        this.outboxService = outboxService;
        this.cruceTurnoService = cruceTurnoService;
        this.historialTurnoService = historialTurnoService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Turno crearTurno(Turno turno, Integer idUsuarioGestor) {
        turno.setActividad(turno.getActividad().trim());
        validar(turno);
        turno.setEstado(Turno.ESTADO_ACTIVO);
        turno.setCuposOcupados(0);

        Turno guardado = turnoRepository.save(turno);

        // HU1 Criterio 3: Auditoría
        historialTurnoService.registrarAccion(
                guardado.getIdTurno(), 
                idUsuarioGestor, 
                "Turno creado. Actividad: " + guardado.getActividad());

        return guardado;
    }

    @Override
    @Transactional
    public Turno actualizarTurno(Integer idTurno, Turno detalles, Integer idUsuarioGestor) {
        Turno turno = turnoRepository.findByIdForUpdate(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con ID: " + idTurno));

        validarTurnoActivo(turno);

        List<TurnoAssignmentRecord> asignacionesVigentes = asignacionRepository.findActiveByTurno(idTurno);

        if (detalles.getCuposTotales() < asignacionesVigentes.size()) {
            throw new BusinessValidationException(
                    "Los cupos no pueden ser menores que las asignaciones vigentes del turno.");
        }

        LocalDate oldDate = turno.getFecha();
        var oldStart = turno.getHoraInicio();
        var oldEnd = turno.getHoraFin();
        String oldActivity = turno.getActividad().trim();

        turno.setFecha(detalles.getFecha());
        turno.setHoraInicio(detalles.getHoraInicio());
        turno.setHoraFin(detalles.getHoraFin());
        turno.setActividad(detalles.getActividad().trim());
        turno.setCuposTotales(detalles.getCuposTotales());

        validar(turno);

        boolean scheduleChanged = !Objects.equals(oldDate, turno.getFecha())
                || !Objects.equals(oldStart, turno.getHoraInicio())
                || !Objects.equals(oldEnd, turno.getHoraFin())
                || !Objects.equals(oldActivity, turno.getActividad());

        if (scheduleChanged) {
            if (!Objects.equals(oldDate, turno.getFecha())
                    || !Objects.equals(oldStart, turno.getHoraInicio())
                    || !Objects.equals(oldEnd, turno.getHoraFin())) {

                // Guardado transaccional temprano para que el servicio de cruce vea el nuevo horario
                turnoRepository.saveAndFlush(turno);
                
                // HU1 Criterios 1 y 2: Revalidar cruces y reportar conflictos en conjunto
                List<String> voluntariosEnConflicto = new ArrayList<>();
                
                for (TurnoAssignmentRecord asignacion : asignacionesVigentes) {
                    List<Turno> cruces = cruceTurnoService.buscarCruces(asignacion.idVoluntario(), idTurno);
                    if (!cruces.isEmpty()) {
                        voluntariosEnConflicto.add(asignacion.nombreVoluntario());
                    }
                }

                if (!voluntariosEnConflicto.isEmpty()) {
                    throw new ConflictException(
                            "No se puede reprogramar el turno. Los siguientes voluntarios presentan cruce de horarios en la nueva fecha/hora: "
                            + String.join(", ", voluntariosEnConflicto));
                }
            }

            enqueueScheduleChangeNotifications(
                    turno,
                    asignacionesVigentes,
                    oldDate,
                    oldStart,
                    oldEnd,
                    oldActivity);
        }

        Turno guardado = turnoRepository.save(turno);

        // HU1 Criterio 3: Auditoría
        String detalleHistorial = "Turno actualizado. Horario anterior: " + formatSchedule(oldDate, oldStart, oldEnd) + 
                                  " | Nuevo horario: " + formatSchedule(guardado.getFecha(), guardado.getHoraInicio(), guardado.getHoraFin()) +
                                  " | Actividad: " + guardado.getActividad();
        historialTurnoService.registrarAccion(guardado.getIdTurno(), idUsuarioGestor, detalleHistorial);

        completarCuposOcupados(List.of(guardado));
        return guardado;
    }

    @Override
    @Transactional
    public Turno cancelarTurno(Integer idTurno, Integer idUsuarioGestor) {
        Turno turno = turnoRepository.findByIdForUpdate(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con ID: " + idTurno));

        validarTurnoActivo(turno);

        List<TurnoAssignmentRecord> asignacionesVigentes = asignacionRepository.findActiveByTurno(idTurno);
        String fechaAnterior = formatSchedule(turno.getFecha(), turno.getHoraInicio(), turno.getHoraFin());

        for (TurnoAssignmentRecord asignacion : asignacionesVigentes) {
            outboxService.enqueue(
                    "shift-cancellation:" + asignacion.idAsignacion(),
                    NotificationTemplateType.CAMBIO_TURNO,
                    asignacion.email(),
                    asignacion.nombreVoluntario(),
                    "Cancelación del turno: " + turno.getActividad() + " (" + fechaAnterior + ")",
                    fechaAnterior);
        }

        asignacionRepository.cancelActiveByTurno(idTurno);
        turno.setEstado(Turno.ESTADO_CANCELADO);
        
        Turno guardado = turnoRepository.save(turno);
        
        // HU1 Criterio 3: Auditoría
        historialTurnoService.registrarAccion(guardado.getIdTurno(), idUsuarioGestor, "Turno cancelado completamente.");

        completarCuposOcupados(List.of(guardado));
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Turno> listarTurnos() {
        List<Turno> turnos = turnoRepository.findAllByOrderByFechaAscHoraInicioAsc();
        completarCuposOcupados(turnos);
        return turnos;
    }

    @Override
    @Transactional(readOnly = true)
    public Turno obtenerTurnoPorId(Integer idTurno) {
        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con ID: " + idTurno));
        
        completarCuposOcupados(List.of(turno));
        return turno;
    }

    private void completarCuposOcupados(List<Turno> turnos) {
        if (turnos.isEmpty()) {
            return;
        }

        Map<Integer, Long> ocupados = turnoRepository
                .contarCuposOcupados(turnos.stream().map(Turno::getIdTurno).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(CupoOcupado::getIdTurno, CupoOcupado::getTotal));

        turnos.forEach(t -> t.setCuposOcupados(ocupados.getOrDefault(t.getIdTurno(), 0L).intValue()));
    }

    private void validar(Turno t) {
        if (!t.getHoraFin().isAfter(t.getHoraInicio())) {
            throw new BusinessValidationException("La hora de fin debe ser posterior a la hora de inicio");
        }

        LocalDate hoy = LocalDate.now(clock);

        if (t.getFecha().isBefore(hoy)) {
            throw new BusinessValidationException("La fecha del turno no puede ser pasada");
        }

        if (!LocalDateTime.of(t.getFecha(), t.getHoraFin()).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessValidationException("El horario del turno ya terminó");
        }
    }

    private void validarTurnoActivo(Turno turno) {
        if (Turno.ESTADO_CANCELADO.equals(turno.getEstado())) {
            throw new BusinessValidationException("El turno ya está cancelado.");
        }
    }

    private void enqueueScheduleChangeNotifications(
            Turno turno,
            List<TurnoAssignmentRecord> asignaciones,
            LocalDate oldDate,
            LocalTime oldStart,
            LocalTime oldEnd,
            String oldActivity) {

        String previousSchedule = formatSchedule(oldDate, oldStart, oldEnd);
        String updatedSchedule = formatSchedule(turno.getFecha(), turno.getHoraInicio(), turno.getHoraFin());

        String activity = "Cambio de turno: " + oldActivity + " (" + previousSchedule + ") -> "
                + turno.getActividad() + " (" + updatedSchedule + ")";

        String transitionKey = Integer.toHexString(Objects.hash(
                oldDate, oldStart, oldEnd, oldActivity,
                turno.getFecha(), turno.getHoraInicio(), turno.getHoraFin(), turno.getActividad()));

        for (TurnoAssignmentRecord asignacion : asignaciones) {
            outboxService.enqueue(
                    "shift-change:" + turno.getIdTurno() + ":" + asignacion.idAsignacion() + ":" + transitionKey,
                    NotificationTemplateType.CAMBIO_TURNO,
                    asignacion.email(),
                    asignacion.nombreVoluntario(),
                    activity,
                    updatedSchedule);
        }
    }

    private String formatSchedule(LocalDate date, LocalTime start, LocalTime end) {
        return date.atTime(start).format(TURN_DATE_FORMAT) + " - " + end.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}