package com.example.backend.service;

import com.example.backend.model.Turno;

import java.time.LocalTime;

/**
 * Regla de cruce de horarios entre turnos (KAN-22 / RF-26-27).
 * Dos turnos se cruzan si son del mismo día y sus horarios se superponen. Los turnos contiguos
 * (uno termina exactamente cuando empieza el otro, p. ej. 08:00-10:00 y 10:00-12:00) NO se cruzan.
 */
public final class ReglaCruceHorario {

    private ReglaCruceHorario() {
    }

    public static boolean seCruzan(Turno a, Turno b) {
        return a.getFecha().equals(b.getFecha())
                && seSuperponen(a.getHoraInicio(), a.getHoraFin(), b.getHoraInicio(), b.getHoraFin());
    }

    /** Intervalos semiabiertos [inicio, fin): se superponen si cada uno empieza antes de que termine el otro. */
    static boolean seSuperponen(LocalTime inicioA, LocalTime finA, LocalTime inicioB, LocalTime finB) {
        return inicioA.isBefore(finB) && inicioB.isBefore(finA);
    }
}
