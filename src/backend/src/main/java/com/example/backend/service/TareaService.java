package com.example.backend.service;

import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;

import java.util.List;

public interface TareaService {
    
    // Criterio 1: Crear tarea en estado Pendiente
    Tarea crearTarea(Tarea tarea);

    // Criterio 2 y 3: Editar tarea validando su estado
    Tarea editarTarea(Integer idTarea, Tarea tareaDetalles);

    // Métodos auxiliares para la gestión
    List<Tarea> listarTareas();
    
    Tarea obtenerTareaPorId(Integer idTarea);

    // Criterio 4: Obtener catálogo de categorías
    List<CategoriaTarea> listarCategorias();
}