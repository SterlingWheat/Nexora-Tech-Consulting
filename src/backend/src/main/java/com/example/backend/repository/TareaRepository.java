package com.example.backend.repository;

import com.example.backend.model.Tarea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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

    // RF-45/46/47 (KAN-56/58): trae la categoría ya cargada con JOIN FETCH, en vez de dejar que
    // Tarea.categoria (LAZY) se resuelva después por fuera de la transacción de este método. Resolverla
    // "después" solo funciona hoy porque Spring Boot mantiene open-in-view activo por defecto; si
    // algún día se desactiva (algo común al endurecer la configuración en producción), acceder a
    // t.getCategoria().getNombre() fuera de esta consulta lanzaría LazyInitializationException.
    @Query("SELECT t FROM Tarea t LEFT JOIN FETCH t.categoria")
    List<Tarea> listarParaReporte();

}