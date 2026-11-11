package com.example.backend.service;

import com.example.backend.exception.BusinessValidationException;
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
import com.example.backend.service.impl.AsignacionTurnoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionTurnoServiceImplTest {

    @Mock
    private AsignacionTurnoRepository asignacionRepository;

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private VoluntarioRepository voluntarioRepository;

    @Mock
    private DisponibilidadRepository disponibilidadRepository;

    @Mock
    private CruceTurnoService cruceTurnoService;

    @Mock
    private NotificationOutboxService outboxService;

    private AsignacionTurnoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AsignacionTurnoServiceImpl(
                asignacionRepository,
                turnoRepository,
                voluntarioRepository,
                disponibilidadRepository,
                cruceTurnoService,
                outboxService);
    }

    // El turno de prueba cae en domingo (25/10/2026); por defecto el voluntario SÍ tiene disponibilidad declarada
    private void stubDisponible(boolean disponible) {
        when(disponibilidadRepository
                .existsByVoluntarioIdVoluntarioAndDiaSemanaAndHoraInicioLessThanEqualAndHoraFinGreaterThanEqual(
                        eq(5), eq(DiaSemana.DOMINGO), eq(LocalTime.of(8, 0)), eq(LocalTime.of(12, 0))))
                .thenReturn(disponible);
    }

    @Test
    void asignarPersisteNuevaFilaYEncolaCorreoSinSobrescribirHistorial() {
        Turno turno = turnoActivo();
        Voluntario voluntario = voluntario();
        TurnoAssignmentRecord created =
                new TurnoAssignmentRecord(100L, 7, 5, "Ana Paz", "ana@example.com", "Pendiente");

        when(turnoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(turno));
        when(asignacionRepository.countActiveByTurno(7)).thenReturn(1);
        when(asignacionRepository.existsActiveByTurnoAndVoluntario(7, 5)).thenReturn(false);
        when(voluntarioRepository.findById(5)).thenReturn(Optional.of(voluntario));
        stubDisponible(true);
        when(asignacionRepository.create(7, 5)).thenReturn(created);

        AsignacionTurnoService.ResultadoAsignacionTurno resultado = service.asignarVoluntarios(7, List.of(5));

        assertEquals(List.of(created), resultado.asignaciones());
        assertTrue(resultado.advertencias().isEmpty());
        verify(asignacionRepository).create(7, 5);
        verify(cruceTurnoService).validarSinCruces(5, 7);
        verify(outboxService).enqueue(
                eq("shift-assignment:100"),
                eq(NotificationTemplateType.ASIGNACION),
                eq("ana@example.com"),
                eq("Ana Paz"),
                eq("Clasificar"),
                eq("25/10/2026 08:00"));
    }

    // KAN-31 Criterio 3: sin disponibilidad declarada, el sistema advierte pero igual asigna
    @Test
    void asignarAdvierteSinBloquearCuandoElVoluntarioNoTieneDisponibilidadDeclarada() {
        Turno turno = turnoActivo();
        Voluntario voluntario = voluntario();
        TurnoAssignmentRecord created =
                new TurnoAssignmentRecord(100L, 7, 5, "Ana Paz", "ana@example.com", "Pendiente");

        when(turnoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(turno));
        when(asignacionRepository.countActiveByTurno(7)).thenReturn(1);
        when(asignacionRepository.existsActiveByTurnoAndVoluntario(7, 5)).thenReturn(false);
        when(voluntarioRepository.findById(5)).thenReturn(Optional.of(voluntario));
        stubDisponible(false);
        when(asignacionRepository.create(7, 5)).thenReturn(created);

        AsignacionTurnoService.ResultadoAsignacionTurno resultado = service.asignarVoluntarios(7, List.of(5));

        assertEquals(List.of(created), resultado.asignaciones());
        assertEquals(1, resultado.advertencias().size());
        assertTrue(resultado.advertencias().get(0).contains("Ana Paz"));
        verify(asignacionRepository).create(7, 5);
        verify(outboxService).enqueue(any(), any(), any(), any(), any(), any());
    }

    @Test
    void asignarRechazaDuplicadoVigenteSinModificarLaFilaAnterior() {
        when(turnoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(turnoActivo()));
        when(asignacionRepository.countActiveByTurno(7)).thenReturn(1);
        when(asignacionRepository.existsActiveByTurnoAndVoluntario(7, 5)).thenReturn(true);
        when(voluntarioRepository.findById(5)).thenReturn(Optional.of(voluntario()));

        assertThrows(BusinessValidationException.class,
                () -> service.asignarVoluntarios(7, List.of(5)));

        verify(asignacionRepository, never()).create(any(), any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void asignarRechazaCuandoNoQuedanCupos() {
        Turno turno = turnoActivo();
        turno.setCuposTotales(1);
        when(turnoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(turno));
        when(asignacionRepository.countActiveByTurno(7)).thenReturn(1);

        assertThrows(BusinessValidationException.class,
                () -> service.asignarVoluntarios(7, List.of(5)));

        verifyNoInteractions(voluntarioRepository, outboxService);
    }

    // KAN-31 Criterio 2 (límite exacto): con el último cupo libre, la asignación sí procede
    @Test
    void asignarProcedeCuandoUsaElUltimoCupoLibre() {
        Turno turno = turnoActivo();
        turno.setCuposTotales(1);
        TurnoAssignmentRecord created =
                new TurnoAssignmentRecord(100L, 7, 5, "Ana Paz", "ana@example.com", "Pendiente");

        when(turnoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(turno));
        when(asignacionRepository.countActiveByTurno(7)).thenReturn(0);
        when(asignacionRepository.existsActiveByTurnoAndVoluntario(7, 5)).thenReturn(false);
        when(voluntarioRepository.findById(5)).thenReturn(Optional.of(voluntario()));
        stubDisponible(true);
        when(asignacionRepository.create(7, 5)).thenReturn(created);

        AsignacionTurnoService.ResultadoAsignacionTurno resultado = service.asignarVoluntarios(7, List.of(5));

        assertEquals(List.of(created), resultado.asignaciones());
    }

    @Test
    void asignarRechazaVoluntarioInactivoSinNotificarNiCrearAsignacion() {
        Voluntario voluntarioInactivo = Voluntario.builder()
                .idVoluntario(5)
                .nombre("Ana")
                .apellido("Paz")
                .email("ana@example.com")
                .activo(false)
                .build();

        when(turnoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(turnoActivo()));
        when(asignacionRepository.countActiveByTurno(7)).thenReturn(0);
        when(voluntarioRepository.findById(5)).thenReturn(Optional.of(voluntarioInactivo));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.asignarVoluntarios(7, List.of(5)));

        assertEquals("El voluntario no est  activo y no puede ser asignado.", ex.getMessage());
        verify(asignacionRepository, never()).create(any(), any());
        verifyNoInteractions(outboxService, cruceTurnoService);
    }

    @Test
    void asignarRechazaVoluntarioConCruceDeHorario() {
        when(turnoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(turnoActivo()));
        when(asignacionRepository.countActiveByTurno(7)).thenReturn(0);
        when(asignacionRepository.existsActiveByTurnoAndVoluntario(7, 5)).thenReturn(false);
        when(voluntarioRepository.findById(5)).thenReturn(Optional.of(voluntario()));
        org.mockito.Mockito.doThrow(new BusinessValidationException("Cruce de horarios"))
                .when(cruceTurnoService).validarSinCruces(5, 7);

        assertThrows(BusinessValidationException.class,
                () -> service.asignarVoluntarios(7, List.of(5)));

        verify(asignacionRepository, never()).create(any(), any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void cancelarAsignacionNotificaYActualizaEstadoSinEliminarRegistro() {
        TurnoAssignmentRecord assignment =
                new TurnoAssignmentRecord(100L, 7, 5, "Ana Paz", "ana@example.com", "Pendiente");

        when(turnoRepository.findByIdForUpdate(7)).thenReturn(Optional.of(turnoActivo()));
        when(asignacionRepository.findActiveByIdAndTurno(100L, 7)).thenReturn(Optional.of(assignment));
        when(asignacionRepository.cancelByIdAndTurno(100L, 7)).thenReturn(1);

        service.cancelarAsignacion(7, 100L);

        verify(outboxService).enqueue(
                eq("shift-assignment-cancellation:100"),
                eq(NotificationTemplateType.CAMBIO_TURNO),
                eq("ana@example.com"),
                eq("Ana Paz"),
                argThat(activity -> activity.contains("Cancelaci") && activity.contains("n")),
                eq("25/10/2026 08:00 - 12:00"));
                
        verify(asignacionRepository).cancelByIdAndTurno(100L, 7);
        verify(asignacionRepository, never()).create(any(), any());
    }

    private Turno turnoActivo() {
        return Turno.builder()
                .idTurno(7)
                .fecha(LocalDate.of(2026, 10, 25))
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .actividad("Clasificar")
                .estado(Turno.ESTADO_ACTIVO)
                .cuposTotales(5)
                .build();
    }

    private Voluntario voluntario() {
        return Voluntario.builder()
                .idVoluntario(5)
                .nombre("Ana")
                .apellido("Paz")
                .email("ana@example.com")
                .activo(true)
                .build();
    }
}