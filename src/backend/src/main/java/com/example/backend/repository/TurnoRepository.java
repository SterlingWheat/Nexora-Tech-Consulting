package com.example.backend.repository;

import com.example.backend.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Integer> {

    List<Turno> findAllByOrderByFechaAscHoraInicioAsc();

    /**
     * Cupos ocupados por turno según asignacion_turno (tabla del esquema del equipo, usada por la asignación
     * de voluntarios). Las asignaciones canceladas o rechazadas liberan el cupo.
     */
    @Query(value = """
            select a.id_turno as "idTurno", count(*) as "total"
            from asignacion_turno a
            where a.id_turno in (:ids)
              and (a.estado is null or a.estado not in ('Cancelada', 'Rechazada'))
            group by a.id_turno
            """, nativeQuery = true)
    List<CupoOcupado> contarCuposOcupados(@Param("ids") Collection<Integer> ids);
}
