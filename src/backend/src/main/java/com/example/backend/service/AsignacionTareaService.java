package com.example.backend.service;

import com.example.backend.model.AsignacionTarea;

import java.util.List;

public interface AsignacionTareaService {
    
    // HU2 Criterio 1 y 2: Asigna voluntarios activos a una tarea y cambia el estado a "Asignada"
    List<AsignacionTarea> asignarVoluntarios(Integer idTarea, List<Integer> idVoluntarios, Integer idUsuarioAsignador);
    
    List<AsignacionTarea> obtenerAsignacionesPorTarea(Integer idTarea);
    
    void cancelarAsignacion(Integer idTarea, Integer idAsignacion, Integer idUsuarioGestor);

    // Nueva funcionalidad: Reemplazar un voluntario asignado por otro
    AsignacionTarea reemplazarVoluntario(Integer idTarea, Integer idAsignacionActual, Integer idNuevoVoluntario, Integer idUsuarioGestor);
}