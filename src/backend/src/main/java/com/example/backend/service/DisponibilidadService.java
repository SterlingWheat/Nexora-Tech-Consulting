package com.example.backend.service;

import com.example.backend.model.Disponibilidad;

import java.util.List;

public interface DisponibilidadService {

    /** Resultado de guardar: las franjas persistidas y los avisos generados (p. ej. franjas unificadas). */
    record Resultado(List<Disponibilidad> franjas, List<String> advertencias) {}

    // Criterio 1: la disponibilidad queda registrada en el perfil del voluntario
    List<Disponibilidad> obtenerDisponibilidad(Integer idVoluntario);

    // Criterio 2 y 3: rechaza fin <= inicio y unifica franjas superpuestas del mismo día
    Resultado reemplazarDisponibilidad(Integer idVoluntario, List<Disponibilidad> franjas);
}
