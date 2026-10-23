package com.example.backend.service;

import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TareaService {
    
    // Crear tarea en estado Pendiente
    Tarea crearTarea(Tarea tarea);

    // Editar tarea validando su estado
    Tarea editarTarea(Integer idTarea, Tarea tareaDetalles);

    // Listado original completo
    List<Tarea> listarTareas();
    
    Tarea obtenerTareaPorId(Integer idTarea);

    // HU1 Criterio 1, 2 y 3: Consulta, filtrado con múltiples variables y paginación
    Page<Tarea> filtrarTareas(String estado, Integer idCategoria, Integer idResponsable, 
                              LocalDateTime fechaInicio, LocalDateTime fechaFin, Pageable pageable);

    // HU3 Criterio 1, 2 y 3: Mover una tarea entre estados controlados y registrar el cambio en el historial
    Tarea cambiarEstado(Integer idTarea, String nuevoEstado, Integer idUsuario);

    // Obtener catálogo de categorías
    List<CategoriaTarea> listarCategorias();
}