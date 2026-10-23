package com.example.backend.service;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Turno;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoServiceImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    // "Ahora" fijo: 2026-10-23 10:00 hora de La Paz
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-23T14:00:00Z"), ZONA);

    @Mock
    private TurnoRepository repository;

    private TurnoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TurnoServiceImpl(repository, RELOJ);
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

    @Test
    void obtener_inexistente_lanzaNoEncontrado() {
        when(repository.findById(9)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerTurnoPorId(9));
    }
}
