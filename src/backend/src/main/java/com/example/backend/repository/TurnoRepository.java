package com.example.backend.repository;

import com.example.backend.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Integer> {

    List<Turno> findAllByOrderByFechaAscHoraInicioAsc();

    /**
     * Cupos ocupados por turno según asignacion_turno (tabla del esquema del equipo, usada por la asignación
     * de voluntarios). Las asignaciones canceladas o rechazadas liberan el cupo.
     */
    @Query(value = "select a.id_turno as \"idTurno\", count(*) as \"total\" "
            + "from asignacion_turno a "
            + "where a.id_turno in (:ids) "
            + "and (a.estado is null or a.estado not in (" + EstadosAsignacion.NO_VIGENTES_SQL + ")) "
            + "group by a.id_turno", nativeQuery = true)
    List<CupoOcupado> contarCuposOcupados(@Param("ids") Collection<Integer> ids);

    /**
     * Turnos vigentes del voluntario en una fecha, sin contar el turno indicado (KAN-22: candidatos a cruce).
     * La comparación de horas la hace {@code ReglaCruceHorario}.
     */
    @Query(value = "select t.* from turno t "
            + "join asignacion_turno a on a.id_turno = t.id_turno "
            + "where a.id_voluntario = :idVoluntario and t.fecha = :fecha and t.id_turno <> :idTurno "
            + "and (a.estado is null or a.estado not in (" + EstadosAsignacion.NO_VIGENTES_SQL + "))",
            nativeQuery = true)
    List<Turno> buscarTurnosVigentesDelVoluntarioEnFecha(@Param("idVoluntario") Integer idVoluntario,
                                                          @Param("fecha") LocalDate fecha,
                                                          @Param("idTurno") Integer idTurno);
}
