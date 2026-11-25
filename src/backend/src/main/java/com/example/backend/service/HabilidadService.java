package com.example.backend.service;

import com.example.backend.model.Habilidad;

import java.util.List;

public interface HabilidadService {

    // Catálogo de habilidades, usado para etiquetar voluntarios y filtrar el calendario de disponibilidad (RF-23)
    List<Habilidad> listarHabilidades();

    Habilidad crearHabilidad(String nombre);

    List<Habilidad> obtenerHabilidadesDeVoluntario(Integer idVoluntario);

    List<Habilidad> asignarHabilidades(Integer idVoluntario, List<Integer> idsHabilidades);
}
