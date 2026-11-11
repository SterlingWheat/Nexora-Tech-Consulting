package com.example.backend.service.impl;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.DiaSemana;
import com.example.backend.model.Turno;
import com.example.backend.model.TurnoAssignmentRecord;
import com.example.backend.model.Voluntario;
import com.example.backend.notification.NotificationOutboxService;
import com.example.backend.repository.AsignacionTurnoRepository;
import com.example.backend.repository.DisponibilidadRepository;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.AsignacionTurnoService;
import com.example.backend.service.CruceTurnoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class AsignacionTurnoServiceImpl implements AsignacionTurnoService {

    private static final DateTimeFormatter TURN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AsignacionTurnoRepository asignacionRepository;
    private final TurnoRepository turnoRepository;
    private final VoluntarioRepository voluntarioRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final CruceTurnoService cruceTurnoService;
    private final NotificationOutboxService outboxService;
    private final Clock clock;

    public AsignacionTurnoServiceImpl(
            AsignacionTurnoRepository asignacionRepository,
            TurnoRepository turnoRepository,
            VoluntarioRepository voluntarioRepository,
            DisponibilidadRepository disponibilidadRepository,
            CruceTurnoService cruceTurnoService,
            NotificationOutboxService outboxService,
            Clock clock) {
        this.asignacionRepository = asignacionRepository;
        this.turnoRepository = turnoRepository;
        this.voluntarioRepository = voluntarioRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.cruceTurnoService = cruceTurnoService;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ResultadoAsignacionTurno asignarVoluntarios(Integer idTurno, List<Integer> idVoluntarios) {
        Turno turno = turnoRepository.findByIdForUpdate(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con ID: " + idTurno));

        validarTurnoActivo(turno);

        if (idVoluntarios == null || idVoluntarios.isEmpty()) {
            throw new BusinessValidationException("Debe proporcionar al menos el ID de un voluntario.");
        }

        if (idVoluntarios.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessValidationException("Los IDs de voluntario deben ser positivos.");
        }

        if (new HashSet<>(idVoluntarios).size() != idVoluntarios.size()) {
            throw new BusinessValidationException("La lista contiene voluntarios duplicados.");
        }

        int ocupados = asignacionRepository.countActiveByTurno(idTurno);
        if (ocupados + idVoluntarios.size() > turno.getCuposTotales()) {
            throw new BusinessValidationException("El turno no tiene suficientes cupos libres.");
        }

        String fechaTurno = fechaTurno(turno);
        DiaSemana diaTurno = DiaSemana.desdeNumero((short) turno.getFecha().getDayOfWeek().getValue());
        List<String> advertencias = new ArrayList<>();

        List<TurnoAssignmentRecord> asignaciones = idVoluntarios.stream().map(idVoluntario -> {
            Voluntario voluntario = voluntarioRepository.findById(idVoluntario)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Voluntario no encontrado con ID: " + idVoluntario));

            validarVoluntario(voluntario);

            if (asignacionRepository.existsActiveByTurnoAndVoluntario(idTurno, idVoluntario)) {
                throw new BusinessValidationException(
                        "El voluntario ya tiene una asignación vigente para este turno.");
            }

            cruceTurnoService.validarSinCruces(idVoluntario, idTurno);

            boolean disponible = disponibilidadRepository
                    .existsByVoluntarioIdVoluntarioAndDiaSemanaAndHoraInicioLessThanEqualAndHoraFinGreaterThanEqual(
                            idVoluntario, diaTurno, turno.getHoraInicio(), turno.getHoraFin());

            if (!disponible) {
                advertencias.add("El voluntario " + nombreCompleto(voluntario)
                        + " no tiene disponibilidad declarada para el horario de este turno.");
            }

            TurnoAssignmentRecord asignacion = asignacionRepository.create(idTurno, idVoluntario);

            outboxService.enqueue(
                    "shift-assignment:" + asignacion.idAsignacion(),
                    NotificationTemplateType.ASIGNACION,
                    voluntario.getEmail().trim(),
                    nombreCompleto(voluntario),
                    turno.getActividad(),
                    fechaTurno);

            return asignacion;
        }).toList();

        return new ResultadoAsignacionTurno(asignaciones, advertencias);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoAssignmentRecord> listarAsignaciones(Integer idTurno) {
        if (!turnoRepository.existsById(idTurno)) {
            throw new ResourceNotFoundException("Turno no encontrado con ID: " + idTurno);
        }
        return asignacionRepository.findAllByTurno(idTurno);
    }

    @Override
    @Transactional
    public void confirmarAsignacion(Integer idTurno, Long idAsignacion) {
        Turno turno = turnoRepository.findByIdForUpdate(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con ID: " + idTurno));
                
        validarTurnoActivo(turno);

        TurnoAssignmentRecord asignacion = asignacionRepository.findActiveByIdAndTurno(idAsignacion, idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una asignación vigente."));

        // HU2 Criterio 1: Confirmar asignación
        if (asignacionRepository.confirmByIdAndTurno(idAsignacion, idTurno) != 1) {
            throw new BusinessValidationException("La asignación no pudo ser confirmada.");
        }
    }

    @Override
    @Transactional
    public void cancelarAsignacion(Integer idTurno, Long idAsignacion) {
        Turno turno = turnoRepository.findByIdForUpdate(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con ID: " + idTurno));

        validarTurnoActivo(turno);

        // HU2 Criterio 3: El sistema impide cancelar si el turno ya ha iniciado
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime inicioTurno = LocalDateTime.of(turno.getFecha(), turno.getHoraInicio());
        if (!ahora.isBefore(inicioTurno)) {
            throw new BusinessValidationException("No se puede cancelar la asignación porque el turno ya ha iniciado.");
        }

        TurnoAssignmentRecord asignacion = asignacionRepository.findActiveByIdAndTurno(idAsignacion, idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una asignación vigente."));

        outboxService.enqueue(
                "shift-assignment-cancellation:" + asignacion.idAsignacion(),
                NotificationTemplateType.CAMBIO_TURNO, // Usa plantilla de cambio/cancelación
                asignacion.email(),
                asignacion.nombreVoluntario(),
                "Cancelación de asignación para el turno de " + turno.getActividad()
                        + " (" + fechaHorario(turno) + ")",
                fechaHorario(turno));

        // HU2 Criterio 2: Al cancelar, el cupo se libera
        if (asignacionRepository.cancelByIdAndTurno(idAsignacion, idTurno) != 1) {
            throw new BusinessValidationException("La asignación cambió antes de poder cancelarse.");
        }
    }

    private void validarTurnoActivo(Turno turno) {
        if (Turno.ESTADO_CANCELADO.equals(turno.getEstado())) {
            throw new BusinessValidationException("No se puede operar sobre un turno cancelado.");
        }
    }

    private void validarVoluntario(Voluntario voluntario) {
        if (!Boolean.TRUE.equals(voluntario.getActivo())) {
            throw new BusinessValidationException("El voluntario no está activo y no puede ser asignado.");
        }
        if (voluntario.getEmail() == null || voluntario.getEmail().isBlank()) {
            throw new BusinessValidationException("El voluntario no tiene un correo válido para recibir la notificación.");
        }
        if (nombreCompleto(voluntario).isBlank()) {
            throw new BusinessValidationException("El voluntario no tiene un nombre válido para la notificación.");
        }
    }

    private String nombreCompleto(Voluntario voluntario) {
        String nombre = voluntario.getNombre() == null ? "" : voluntario.getNombre().trim();
        String apellido = voluntario.getApellido() == null ? "" : voluntario.getApellido().trim();
        return (nombre + " " + apellido).trim();
    }

    private String fechaTurno(Turno turno) {
        return turno.getFecha().atTime(turno.getHoraInicio()).format(TURN_DATE_FORMAT);
    }

    private String fechaHorario(Turno turno) {
        return fechaTurno(turno) + " - " + turno.getHoraFin().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}