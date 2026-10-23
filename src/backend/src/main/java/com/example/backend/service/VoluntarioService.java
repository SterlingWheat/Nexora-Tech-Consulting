package com.example.backend.service;

import com.example.backend.model.Voluntario;

import java.util.List;

public interface VoluntarioService {

    // Criterio 1 y 2: registrar como activo, rechazando documento o correo duplicado
    Voluntario registrarVoluntario(Voluntario voluntario);

    // Criterio 3: actualizar datos y registrar fecha de modificación
    Voluntario actualizarVoluntario(Integer idVoluntario, Voluntario detalles);

    List<Voluntario> listarVoluntarios();

    Voluntario obtenerVoluntarioPorId(Integer idVoluntario);
}
