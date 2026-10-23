package com.example.backend.repository;

import com.example.backend.model.Tarea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TareaRepository extends JpaRepository<Tarea, Integer> {

    // HU1 Criterio 1 y 2: Filtrado y paginación por estado, categoría, responsable (voluntario) y rango de fechas
    @Query("SELECT DISTINCT t FROM Tarea t " +
           "LEFT JOIN AsignacionTarea a ON a.tarea.idTarea = t.idTarea " +
           "WHERE (:estado IS NULL OR t.estado = :estado) " +
           "AND (:idCategoria IS NULL OR t.categoria.idCategoria = :idCategoria) " +
           "AND (:idResponsable IS NULL OR a.voluntario.idVoluntario = :idResponsable) " +
           "AND (CAST(:fechaInicio AS timestamp) IS NULL OR t.fechaCreacion >= :fechaInicio) " +
           "AND (CAST(:fechaFin AS timestamp) IS NULL OR t.fechaCreacion <= :fechaFin)")
    Page<Tarea> filtrarTareas(
            @Param("estado") String estado,
            @Param("idCategoria") Integer idCategoria,
            @Param("idResponsable") Integer idResponsable,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );
    
}