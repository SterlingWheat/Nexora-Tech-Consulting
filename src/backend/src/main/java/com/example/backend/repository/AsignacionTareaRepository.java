package com.example.backend.repository;

import com.example.backend.model.AsignacionTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsignacionTareaRepository extends JpaRepository<AsignacionTarea, Integer> {
    
    List<AsignacionTarea> findByTareaIdTarea(Integer idTarea);
    
    List<AsignacionTarea> findByVoluntarioIdVoluntario(Integer idVoluntario);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM AsignacionTarea a
            WHERE a.tarea.idTarea = :idTarea
              AND a.voluntario.idVoluntario = :idVoluntario
              AND COALESCE(a.estadoRespuesta, 'Asignado') NOT IN ('Cancelada', 'Rechazada')
            """)
    boolean existsActiveByTareaAndVoluntario(
            @Param("idTarea") Integer idTarea,
            @Param("idVoluntario") Integer idVoluntario);

    @Query("""
            SELECT COUNT(a)
            FROM AsignacionTarea a
            WHERE a.tarea.idTarea = :idTarea
              AND COALESCE(a.estadoRespuesta, 'Asignado') NOT IN ('Cancelada', 'Rechazada')
            """)
    long countActiveByTareaId(@Param("idTarea") Integer idTarea);
}