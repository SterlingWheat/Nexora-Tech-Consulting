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

    // HU1 Criterio 1 y 2: Filtrado y paginación por estado, categoría, responsable (voluntario) y rango de fechas.
    // RF-44 (KAN-49) Criterio 2: ":abierta" permite llegar aquí desde el dashboard ya filtrado
    // (true = tareas sin llegar a un estado terminal). ":estadosTerminales" se recibe como parámetro
    // (Tarea.ESTADOS_TERMINALES) en vez de ir hardcodeado aquí, para que exista una sola fuente de verdad.
    @Query("SELECT DISTINCT t FROM Tarea t " +
           "LEFT JOIN AsignacionTarea a ON a.tarea.idTarea = t.idTarea " +
           "WHERE (:estado IS NULL OR t.estado = :estado) " +
           "AND (:idCategoria IS NULL OR t.categoria.idCategoria = :idCategoria) " +
           "AND (:idResponsable IS NULL OR a.voluntario.idVoluntario = :idResponsable) " +
           "AND (CAST(:fechaInicio AS timestamp) IS NULL OR t.fechaCreacion >= :fechaInicio) " +
           "AND (CAST(:fechaFin AS timestamp) IS NULL OR t.fechaCreacion <= :fechaFin) " +
           "AND (:abierta IS NULL " +
           "     OR (:abierta = TRUE AND t.estado NOT IN :estadosTerminales) " +
           "     OR (:abierta = FALSE AND t.estado IN :estadosTerminales))")
    Page<Tarea> filtrarTareas(
            @Param("estado") String estado,
            @Param("idCategoria") Integer idCategoria,
            @Param("idResponsable") Integer idResponsable,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("abierta") Boolean abierta,
            @Param("estadosTerminales") List<String> estadosTerminales,
            Pageable pageable
    );

    // RF-44 (KAN-49): tareas que todavía no llegaron a un estado terminal
    @Query("SELECT COUNT(t) FROM Tarea t WHERE t.estado NOT IN :estadosTerminales")
    long contarAbiertas(@Param("estadosTerminales") List<String> estadosTerminales);

    // RF-44 (KAN-49): desglose para el gráfico de tareas por estado del dashboard
    @Query("SELECT t.estado AS estado, COUNT(t) AS cantidad FROM Tarea t GROUP BY t.estado")
    List<ConteoPorEstado> contarPorEstado();

    // RF-45/46/47 (KAN-56/58): trae la categoría ya cargada con JOIN FETCH, en vez de dejar que
    // Tarea.categoria (LAZY) se resuelva después por fuera de la transacción de este método. Resolverla
    // "después" solo funciona hoy porque Spring Boot mantiene open-in-view activo por defecto; si
    // algún día se desactiva (algo común al endurecer la configuración en producción), acceder a
    // t.getCategoria().getNombre() fuera de esta consulta lanzaría LazyInitializationException.
    @Query("SELECT t FROM Tarea t LEFT JOIN FETCH t.categoria")
    List<Tarea> listarParaReporte();

}