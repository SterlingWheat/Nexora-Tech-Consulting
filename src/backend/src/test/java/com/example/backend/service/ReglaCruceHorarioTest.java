package com.example.backend.service;

import com.example.backend.model.Turno;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Criterio 4 de KAN-22: casos límite de la regla de cruce de horarios. */
class ReglaCruceHorarioTest {

    private static final String DIA = "2026-10-28";

    private static Turno turno(String fecha, String inicio, String fin) {
        return Turno.builder().fecha(LocalDate.parse(fecha)).horaInicio(LocalTime.parse(inicio))
                .horaFin(LocalTime.parse(fin)).actividad("X").cuposTotales(1).build();
    }

    private static void assertCruce(boolean esperado, Turno a, Turno b) {
        assertEquals(esperado, ReglaCruceHorario.seCruzan(a, b));
        // La regla es simétrica: el orden de los turnos no cambia el resultado
        assertEquals(esperado, ReglaCruceHorario.seCruzan(b, a));
    }

    // Criterio 1
    @Test
    void superposicionParcial_seCruzan() {
        assertCruce(true, turno(DIA, "08:00", "12:00"), turno(DIA, "10:00", "14:00"));
    }

    @Test
    void unoContenidoEnOtro_seCruzan() {
        assertCruce(true, turno(DIA, "08:00", "18:00"), turno(DIA, "10:00", "12:00"));
    }

    @Test
    void horariosIdenticos_seCruzan() {
        assertCruce(true, turno(DIA, "08:00", "12:00"), turno(DIA, "08:00", "12:00"));
    }

    @Test
    void mismaHoraDeInicio_seCruzan() {
        assertCruce(true, turno(DIA, "08:00", "10:00"), turno(DIA, "08:00", "12:00"));
    }

    @Test
    void mismaHoraDeFin_seCruzan() {
        assertCruce(true, turno(DIA, "08:00", "12:00"), turno(DIA, "10:00", "12:00"));
    }

    @Test
    void unMinutoDeSuperposicion_seCruzan() {
        assertCruce(true, turno(DIA, "08:00", "10:01"), turno(DIA, "10:00", "12:00"));
    }

    // Criterio 3
    @Test
    void turnosContiguos_noSeCruzan() {
        assertCruce(false, turno(DIA, "08:00", "10:00"), turno(DIA, "10:00", "12:00"));
    }

    @Test
    void unMinutoDeSeparacion_noSeCruzan() {
        assertCruce(false, turno(DIA, "08:00", "09:59"), turno(DIA, "10:00", "12:00"));
    }

    @Test
    void turnosSeparadosElMismoDia_noSeCruzan() {
        assertCruce(false, turno(DIA, "08:00", "10:00"), turno(DIA, "14:00", "18:00"));
    }

    @Test
    void mismoHorarioEnDiasDistintos_noSeCruzan() {
        assertCruce(false, turno("2026-10-28", "08:00", "12:00"), turno("2026-10-29", "08:00", "12:00"));
    }

    @Test
    void finDelDiaYComienzoDelSiguiente_noSeCruzan() {
        assertCruce(false, turno("2026-10-28", "20:00", "23:59"), turno("2026-10-29", "00:00", "04:00"));
    }

    @Test
    void turnoMuyCorto_seCruzaConUnoQueLoContiene() {
        assertCruce(true, turno(DIA, "09:00", "09:01"), turno(DIA, "08:00", "10:00"));
    }
}
