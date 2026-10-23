package com.example.backend.service;

import com.example.backend.model.HistorialTarea;

import java.util.List;

public interface HistorialTareaService {
    
    // HU2 Criterio 3 y HU3 Criterio 2: Registra quién hizo la acción, cuándo y el detalle del cambio
    HistorialTarea registrarAccion(Integer idTarea, Integer idUsuario, String detalle);

    List<HistorialTarea> obtenerHistorialPorTarea(Integer idTarea);
}