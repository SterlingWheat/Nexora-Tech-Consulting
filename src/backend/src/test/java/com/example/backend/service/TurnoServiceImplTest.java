package com.example.backend.service;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mail.NotificationTemplateType;
import com.example.backend.model.Turno;
import com.example.backend.model.TurnoAssignmentRecord;
import com.example.backend.notification.NotificationOutboxService;
import com.example.backend.repository.AsignacionTurnoRepository;
import com.example.backend.repository.CupoOcupado;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.service.impl.TurnoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoServiceImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // "Ahora" fijo: 2026-10-23 10:00 hora de La Paz
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-23T14:00:00Z"), ZONA);

    @Mock
    private TurnoRepository repository;

    @Mock
    private AsignacionTurnoRepository asignacionRepository;

    @Mock
    private NotificationOutboxService outboxService;

    @Mock
    private CruceTurnoService cruceTurnoService;

    private TurnoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TurnoServiceImpl(repository, asignacionRepository, outboxService, cruceTurnoService, RELOJ);
    }

    private Turno turno(String fecha, String inicio, String fin, int cupos) {
        return Turno.builder().fecha(LocalDate.parse(fecha)).horaInicio(LocalTime.parse(inicio))
                .horaFin(LocalTime.parse(fin)).actividad("  Clasificar alimentos ").cuposTotales(cupos).build();
    }

    // Criterio 1
    @Test
    void crear_conDatosValidos_creaConTodosLosCuposLibres() {
        when(repository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        Turno creado = service.crearTurno(turno("2026-10-25", "08:00", "12:00", 5));

        assertEquals("Clasificar alimentos", creado.getActividad());
        assertEquals(0, creado.getCuposOcupados());
        assertEquals(5, creado.getCuposLibres());
    }

    // Criterio 2
    @Test
    void crear_fechaPasada_seRechaza() {
        assertThrows(BusinessValidationException.class, () -> service.crearTurno(turno("2026-10-22", "08:00", "12:00", 5)));
        verify(repository, never()).save(any());
    }

    // Criterio 2
    @Test
    void crear_horaFinAnteriorOIgualAInicio_seRechaza() {
        assertThrows(BusinessValidationException.class, () -> service.crearTurno(turno("2026-10-25", "12:00", "08:00", 5)));
        assertThrows(BusinessValidationException.class, () -> service.crearTurno(turno("2026-10-25", "08:00", "08:00", 5)));
        verify(repository, never()).save(any());
    }

    // Criterio 2 (borde)
    @Test
    void crear_hoyConHorarioYaTerminado_seRechaza() {
        assertThrows(BusinessValidationException.class, () -> service.crearTurno(turno("2026-10-23", "07:00", "09:00", 5)));
    }

    @Test
    void crear_hoyConHorarioQueAunNoTermina_seAcepta() {
        when(repository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> service.crearTurno(turno("2026-10-23", "09:00", "13:00", 3)));
    }

    // Criterio 3
    @Test
    void cuposLibres_seCalculanComoTotalesMenosOcupados() {
        Turno t = turno("2026-10-25", "08:00", "12:00", 10);
        t.setCuposOcupados(4);

        assertEquals(6, t.getCuposLibres());
    }

    @Test
    void crear_ignoraCuposOcupadosEnviados() {
        when(repository.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));
        Turno t = turno("2026-10-25", "08:00", "12:00", 10);
        t.setCuposOcupados(9);

        assertEquals(0, service.crearTurno(t).getCuposOcupados());
    }

    @Test
    void listar_devuelveLoOrdenadoDelRepositorio() {
        var t = turno("2026-10-25", "08:00", "12:00", 2);
        when(repository.findAllByOrderByFechaAscHoraInicioAsc()).thenReturn(List.of(t));

        assertEquals(List.of(t), service.listarTurnos());
    }

    // Criterio 3: los ocupados se cuentan desde las asignaciones del turno
    @Test
    void listar_completaCuposOcupadosDesdeLasAsignaciones() {
        var t1 = turno("2026-10-25", "08:00", "12:00", 5);
        t1.setIdTurno(1);
        var t2 = turno("2026-10-26", "08:00", "12:00", 3);
        t2.setIdTurno(2);
        when(repository.findAllByOrderByFechaAscHoraInicioAsc()).thenReturn(List.of(t1, t2));
        when(repository.contarCuposOcupados(List.of(1, 2))).thenReturn(List.of(ocupado(1, 2L)));

        var resultado = service.listarTurnos();

        assertEquals(2, resultado.get(0).getCuposOcupados());
        assertEquals(3, resultado.get(0).getCuposLibres());
        assertEquals(0, resultado.get(1).getCuposOcupados());
        assertEquals(3, resultado.get(1).getCuposLibres());
    }

    @Test
    void listar_sinTurnos_noConsultaAsignaciones() {
        when(repository.findAllByOrderByFechaAscHoraInicioAsc()).thenReturn(List.of());

        assertTrue(service.listarTurnos().isEmpty());
        verify(repository, never()).contarCuposOcupados(any());
    }

    @Test
    void obtener_completaCuposOcupados() {
        var t = turno("2026-10-25", "08:00", "12:00", 5);
        t.setIdTurno(7);
        when(repository.findById(7)).thenReturn(Optional.of(t));
        when(repository.contarCuposOcupados(List.of(7))).thenReturn(List.of(ocupado(7, 5L)));

        assertEquals(0, service.obtenerTurnoPorId(7).getCuposLibres());
    }

    private static CupoOcupado ocupado(int idTurno, long total) {
        return new CupoOcupado() {
            public Integer getIdTurno() { return idTurno; }
            public Long getTotal() { return total; }
        };
    }

    @Test
    void obtener_inexistente_lanzaNoEncontrado() {
        when(repository.findById(9)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerTurnoPorId(9));
    }

    @Test
    void actualizar_cambioDeHorarioEncolaCorreoParaCadaAsignacionVigente() {
        Turno existente = turno("2026-10-25", "08:00", "12:00", 5);
        existente.setIdTurno(7);
        existente.setEstado(Turno.ESTADO_ACTIVO);
        TurnoAssignmentRecord assignment =
                new TurnoAssignmentRecord(42L, 7, 5, "Ana Paz", "ana@example.com", "Pendiente");

        when(repository.findByIdForUpdate(7)).thenReturn(Optional.of(existente));
        when(asignacionRepository.findActiveByTurno(7)).thenReturn(List.of(assignment));
        when(repository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.contarCuposOcupados(List.of(7))).thenReturn(List.of());

        Turno resultado = service.actualizarTurno(7, turno("2026-10-26", "09:00", "13:00", 5));

        assertEquals(LocalDate.parse("2026-10-26"), resultado.getFecha());
        verify(outboxService).enqueue(
                org.mockito.ArgumentMatchers.argThat(key -> key.startsWith("shift-change:7:42:")),
                org.mockito.ArgumentMatchers.eq(NotificationTemplateType.CAMBIO_TURNO),
                org.mockito.ArgumentMatchers.eq("ana@example.com"),
                org.mockito.ArgumentMatchers.eq("Ana Paz"),
                org.mockito.ArgumentMatchers.argThat(activity ->
                        activity.contains("25/10/2026") && activity.contains("26/10/2026")),
                org.mockito.ArgumentMatchers.contains("26/10/2026"));
        verify(cruceTurnoService).validarSinCruces(5, 7);
    }

    @Test
    void actualizar_noPermiteReducirCuposDebajoDeAsignacionesVigentes() {
        Turno existente = turno("2026-10-25", "08:00", "12:00", 5);
        existente.setIdTurno(7);
        existente.setEstado(Turno.ESTADO_ACTIVO);

        when(repository.findByIdForUpdate(7)).thenReturn(Optional.of(existente));
        when(asignacionRepository.findActiveByTurno(7)).thenReturn(List.of(
                new TurnoAssignmentRecord(42L, 7, 5, "Ana Paz", "ana@example.com", "Pendiente"),
                new TurnoAssignmentRecord(43L, 7, 6, "Luis Paz", "luis@example.com", "Pendiente")));

        assertThrows(BusinessValidationException.class,
                () -> service.actualizarTurno(7, turno("2026-10-25", "08:00", "12:00", 1)));

        verify(repository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void actualizar_rechazaHorarioQueGeneraCruceAntesDeEncolarAvisos() {
        Turno existente = turno("2026-10-25", "08:00", "12:00", 5);
        existente.setIdTurno(7);
        existente.setEstado(Turno.ESTADO_ACTIVO);
        TurnoAssignmentRecord assignment =
                new TurnoAssignmentRecord(42L, 7, 5, "Ana Paz", "ana@example.com", "Pendiente");

        when(repository.findByIdForUpdate(7)).thenReturn(Optional.of(existente));
        when(asignacionRepository.findActiveByTurno(7)).thenReturn(List.of(assignment));
        when(repository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        doThrow(new com.example.backend.exception.ConflictException("Cruce de horarios"))
                .when(cruceTurnoService).validarSinCruces(5, 7);

        assertThrows(com.example.backend.exception.ConflictException.class,
                () -> service.actualizarTurno(7, turno("2026-10-26", "09:00", "13:00", 5)));

        verify(outboxService, never()).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelar_mantieneElTurnoYLasAsignacionesEnHistorialYEncolaAvisos() {
        Turno existente = turno("2026-10-25", "08:00", "12:00", 5);
        existente.setIdTurno(7);
        existente.setEstado(Turno.ESTADO_ACTIVO);
        TurnoAssignmentRecord assignment =
                new TurnoAssignmentRecord(42L, 7, 5, "Ana Paz", "ana@example.com", "Pendiente");

        when(repository.findByIdForUpdate(7)).thenReturn(Optional.of(existente));
        when(asignacionRepository.findActiveByTurno(7)).thenReturn(List.of(assignment));
        when(asignacionRepository.cancelActiveByTurno(7)).thenReturn(1);
        when(repository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.contarCuposOcupados(List.of(7))).thenReturn(List.of());

        Turno resultado = service.cancelarTurno(7);

        assertEquals(Turno.ESTADO_CANCELADO, resultado.getEstado());
        verify(asignacionRepository).cancelActiveByTurno(7);
        verify(repository, never()).delete(any());
        verify(outboxService).enqueue(
                org.mockito.ArgumentMatchers.eq("shift-cancellation:42"),
                org.mockito.ArgumentMatchers.eq(NotificationTemplateType.CAMBIO_TURNO),
                org.mockito.ArgumentMatchers.eq("ana@example.com"),
                org.mockito.ArgumentMatchers.eq("Ana Paz"),
                argThat(activity -> activity.contains("Cancelaci") && activity.contains("n del turno")),
                org.mockito.ArgumentMatchers.contains("25/10/2026"));
    }
}