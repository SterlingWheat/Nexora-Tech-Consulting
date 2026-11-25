package com.example.backend.service;

import com.example.backend.model.DiaSemana;
import com.example.backend.model.Disponibilidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DisponibilidadService {

    /** Resultado de guardar: las franjas persistidas y los avisos generados (p. ej. franjas unificadas). */
    record Resultado(List<Disponibilidad> franjas, List<String> advertencias) {}

    /** RF-23: una franja de disponibilidad de un voluntario, ubicada en una fecha concreta del calendario. */
    record Ocurrencia(Integer idVoluntario, String nombreVoluntario, String apellidoVoluntario,
                       LocalDate fecha, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin) {}

    // Criterio 1: la disponibilidad queda registrada en el perfil del voluntario
    List<Disponibilidad> obtenerDisponibilidad(Integer idVoluntario);

    // Criterio 2 y 3: rechaza fin <= inicio y unifica franjas superpuestas del mismo día
    Resultado reemplazarDisponibilidad(Integer idVoluntario, List<Disponibilidad> franjas);

    // RF-23 Criterios 1 y 2: franjas por voluntario, dentro de un rango de fechas, con filtro opcional de voluntario/habilidad
    List<Ocurrencia> obtenerCalendario(LocalDate desde, LocalDate hasta, Integer idVoluntario, Long idHabilidad);
}
