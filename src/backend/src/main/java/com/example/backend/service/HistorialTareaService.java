package com.example.backend.service;

import com.example.backend.model.HistorialTarea;

import java.util.List;

public interface HistorialTareaService {
    
    // HU2: Método de legado mantenido por compatibilidad con llamados antiguos
    HistorialTarea registrarAccion(Integer idTarea, Integer idUsuario, String detalle);

    // HU2 Criterio 1: Nueva firma para registrar trazabilidad operativa detallada (con tipo y valores)
    HistorialTarea registrarAccionDetallada(Integer idTarea, Integer idUsuario, String tipoCambio, String detalle, String valorAnterior, String valorNuevo);

    List<HistorialTarea> obtenerHistorialPorTarea(Integer idTarea);
}