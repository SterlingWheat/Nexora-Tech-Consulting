package com.example.backend.service.impl;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.Turno;
import com.example.backend.model.TurnoAssignmentRecord;
import com.example.backend.repository.AsignacionTurnoRepository;
import com.example.backend.repository.CupoOcupado;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.notification.NotificationOutboxService;
import com.example.backend.service.CruceTurnoService;
import com.example.backend.service.TurnoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private final Clock clock;

    public TurnoServiceImpl(
            TurnoRepository turnoRepository,
            AsignacionTurnoRepository asignacionRepository,
            NotificationOutboxService outboxService,
            CruceTurnoService cruceTurnoService,
            Clock clock) {
        this.turnoRepository = turnoRepository;
        this.asignacionRepository = asignacionRepository;
        this.outboxService = outboxService;
        this.cruceTurnoService = cruceTurnoService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Turno crearTurno(Turno turno) {
        turno.setActividad(turno.getActividad().trim());
        validar(turno);
        turno.setEstado(Turno.ESTADO_ACTIVO);
        turno.setCuposOcupados(0);
        return turnoRepository.save(turno);
    }

    @Override
    @Transactional
    public Turno actualizarTurno(Integer idTurno, Turno detalles) {
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
        Turno guardado = turnoRepository.save(turno);
        if (scheduleChanged) {
            if (!Objects.equals(oldDate, turno.getFecha())
                    || !Objects.equals(oldStart, turno.getHoraInicio())
                    || !Objects.equals(oldEnd, turno.getHoraFin())) {
                turnoRepository.flush();
                asignacionesVigentes.forEach(asignacion ->
                        cruceTurnoService.validarSinCruces(asignacion.idVoluntario(), idTurno));
            }
            enqueueScheduleChangeNotifications(
                    turno,
                    asignacionesVigentes,
                    oldDate,
                    oldStart,
                    oldEnd,
                    oldActivity);
        }
        completarCuposOcupados(List.of(guardado));
        return guardado;
    }

    @Override
    @Transactional
    public Turno cancelarTurno(Integer idTurno) {
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

    // Criterio 3: los cupos ocupados salen de las asignaciones vigentes del turno
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

    // Criterio 2
    private void validar(Turno t) {
        if (!t.getHoraFin().isAfter(t.getHoraInicio())) {
            throw new BusinessValidationException("La hora de fin debe ser posterior a la hora de inicio");
        }
        LocalDate hoy = LocalDate.now(clock);
        if (t.getFecha().isBefore(hoy)) {
            throw new BusinessValidationException("La fecha del turno no puede ser pasada");
        }
        // Un turno de hoy cuyo horario ya terminó equivale a una fecha pasada
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
