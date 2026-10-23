package com.example.backend.service;

import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Turno;
import com.example.backend.repository.TurnoRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.impl.CruceTurnoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CruceTurnoServiceImplTest {

    private static final LocalDate DIA = LocalDate.parse("2026-10-28");

    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private VoluntarioRepository voluntarioRepository;

    private CruceTurnoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CruceTurnoServiceImpl(turnoRepository, voluntarioRepository);
    }

    private static Turno turno(int id, String actividad, String inicio, String fin) {
        return Turno.builder().idTurno(id).fecha(DIA).horaInicio(LocalTime.parse(inicio))
                .horaFin(LocalTime.parse(fin)).actividad(actividad).cuposTotales(5).build();
    }

    /** El turno que se quiere asignar existe y el voluntario ya tiene los turnos indicados. */
    private void escenario(Turno nuevo, Turno... yaAsignados) {
        when(voluntarioRepository.existsById(1)).thenReturn(true);
        when(turnoRepository.findById(nuevo.getIdTurno())).thenReturn(Optional.of(nuevo));
        when(turnoRepository.buscarTurnosVigentesDelVoluntarioEnFecha(1, DIA, nuevo.getIdTurno()))
                .thenReturn(List.of(yaAsignados));
    }

    // Criterio 1
    @Test
    void validar_turnoSuperpuesto_seBloquea() {
        escenario(turno(2, "Atención", "10:00", "14:00"), turno(1, "Clasificar alimentos", "08:00", "12:00"));

        assertThrows(ConflictException.class, () -> service.validarSinCruces(1, 2));
    }

    // Criterio 2
    @Test
    void validar_mensajeIndicaElTurnoConElQueChoca() {
        escenario(turno(2, "Atención", "10:00", "14:00"), turno(1, "Clasificar alimentos", "08:00", "12:00"));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.validarSinCruces(1, 2));

        assertTrue(ex.getMessage().contains("Clasificar alimentos"));
        assertTrue(ex.getMessage().contains("2026-10-28"));
        assertTrue(ex.getMessage().contains("08:00"));
        assertTrue(ex.getMessage().contains("12:00"));
    }

    @Test
    void validar_variosCruces_losListaTodosOrdenadosPorHora() {
        escenario(turno(9, "Cocina", "08:00", "18:00"),
                turno(3, "Tarde", "14:00", "16:00"),
                turno(1, "Mañana", "08:00", "10:00"));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.validarSinCruces(1, 9));

        assertTrue(ex.getMessage().indexOf("Mañana") < ex.getMessage().indexOf("Tarde"));
    }

    // Criterio 3
    @Test
    void validar_turnoContiguo_sePermite() {
        escenario(turno(2, "Atención", "10:00", "12:00"), turno(1, "Clasificar", "08:00", "10:00"));

        assertDoesNotThrow(() -> service.validarSinCruces(1, 2));
        assertTrue(service.buscarCruces(1, 2).isEmpty());
    }

    @Test
    void validar_soloLosContiguosYLosQueNoSeCruzan_sePermite() {
        escenario(turno(2, "Atención", "10:00", "12:00"),
                turno(1, "Antes", "08:00", "10:00"),
                turno(3, "Después", "12:00", "14:00"),
                turno(4, "Noche", "20:00", "22:00"));

        assertDoesNotThrow(() -> service.validarSinCruces(1, 2));
    }

    @Test
    void validar_unSoloTurnoQueChocaEntreVarios_bloqueaSoloPorEse() {
        escenario(turno(2, "Atención", "10:00", "12:00"),
                turno(1, "Antes", "08:00", "10:00"),
                turno(3, "Choca", "11:00", "13:00"));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.validarSinCruces(1, 2));

        assertTrue(ex.getMessage().contains("Choca"));
        assertFalse(ex.getMessage().contains("Antes"));
    }

    @Test
    void validar_sinTurnosAsignados_sePermite() {
        escenario(turno(2, "Atención", "10:00", "12:00"));

        assertDoesNotThrow(() -> service.validarSinCruces(1, 2));
    }

    @Test
    void validar_voluntarioInexistente_lanzaNoEncontrado() {
        when(voluntarioRepository.existsById(9)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.validarSinCruces(9, 2));
        verify(turnoRepository, never()).buscarTurnosVigentesDelVoluntarioEnFecha(any(), any(), any());
    }

    @Test
    void validar_turnoInexistente_lanzaNoEncontrado() {
        when(voluntarioRepository.existsById(1)).thenReturn(true);
        when(turnoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.validarSinCruces(1, 99));
    }
}
