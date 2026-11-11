package com.example.backend.repository;

import com.example.backend.model.TurnoAssignmentRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AsignacionTurnoRepository {

    private static final String SELECT_COLUMNS = """
            select a.id_asignacion, a.id_turno, a.id_voluntario,
                   trim(concat(coalesce(v.nombres, ''), ' ', coalesce(v.apellido, ''))) as nombre_voluntario,
                   v.email, a.estado
            from asignacion_turno a
            join voluntario v on v.id_voluntario = a.id_voluntario
            """;

    private static final String ESTADOS_VIGENTES =
            "(a.estado is null or a.estado not in (" + EstadosAsignacion.NO_VIGENTES_SQL + "))";

    private static final RowMapper<TurnoAssignmentRecord> ROW_MAPPER = (resultSet, rowNumber) ->
            new TurnoAssignmentRecord(
                    resultSet.getLong("id_asignacion"),
                    resultSet.getInt("id_turno"),
                    resultSet.getInt("id_voluntario"),
                    resultSet.getString("nombre_voluntario"),
                    resultSet.getString("email"),
                    resultSet.getString("estado"));

    private final JdbcTemplate jdbcTemplate;

    public AsignacionTurnoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countActiveByTurno(Integer idTurno) {
        return jdbcTemplate.queryForObject(
                "select count(*) from asignacion_turno a where a.id_turno = ? and "
                        + ESTADOS_VIGENTES,
                Integer.class,
                idTurno);
    }

    public boolean existsActiveByTurnoAndVoluntario(Integer idTurno, Integer idVoluntario) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from asignacion_turno a where a.id_turno = ? "
                        + "and a.id_voluntario = ? and " + ESTADOS_VIGENTES,
                Integer.class,
                idTurno,
                idVoluntario);
        return count != null && count > 0;
    }

    public TurnoAssignmentRecord create(Integer idTurno, Integer idVoluntario) {
        Long idAsignacion = jdbcTemplate.queryForObject(
                "insert into asignacion_turno (id_turno, id_voluntario, estado) "
                        + "values (?, ?, ?) returning id_asignacion",
                Long.class,
                idTurno,
                idVoluntario,
                EstadosAsignacion.PENDIENTE);

        if (idAsignacion == null) {
            throw new IllegalStateException("No se pudo generar el ID de la asignación del turno.");
        }

        return findById(idAsignacion)
                .orElseThrow(() -> new IllegalStateException("No se encontró la asignación recién creada."));
    }

    public Optional<TurnoAssignmentRecord> findById(Long idAsignacion) {
        List<TurnoAssignmentRecord> result = jdbcTemplate.query(
                SELECT_COLUMNS + "where a.id_asignacion = ?",
                ROW_MAPPER,
                idAsignacion);
        return result.stream().findFirst();
    }

    public Optional<TurnoAssignmentRecord> findActiveByIdAndTurno(Long idAsignacion, Integer idTurno) {
        List<TurnoAssignmentRecord> result = jdbcTemplate.query(
                SELECT_COLUMNS + "where a.id_asignacion = ? and a.id_turno = ? and "
                        + ESTADOS_VIGENTES,
                ROW_MAPPER,
                idAsignacion,
                idTurno);
        return result.stream().findFirst();
    }

    public List<TurnoAssignmentRecord> findAllByTurno(Integer idTurno) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + "where a.id_turno = ? order by a.id_asignacion",
                ROW_MAPPER,
                idTurno);
    }

    public List<TurnoAssignmentRecord> findActiveByTurno(Integer idTurno) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + "where a.id_turno = ? and " + ESTADOS_VIGENTES
                        + " order by a.id_asignacion",
                ROW_MAPPER,
                idTurno);
    }

    // HU2 Criterio 1: Confirmar asignación
    public int confirmByIdAndTurno(Long idAsignacion, Integer idTurno) {
        return jdbcTemplate.update(
                "update asignacion_turno a set estado = ? "
                        + "where a.id_asignacion = ? and a.id_turno = ? and " + ESTADOS_VIGENTES,
                EstadosAsignacion.CONFIRMADA,
                idAsignacion,
                idTurno);
    }

    // HU2 Criterio 2: Cancelar asignación y liberar cupo (ya gestionado por ESTADOS_VIGENTES)
    public int cancelByIdAndTurno(Long idAsignacion, Integer idTurno) {
        return jdbcTemplate.update(
                "update asignacion_turno a set estado = ? "
                        + "where a.id_asignacion = ? and a.id_turno = ? and " + ESTADOS_VIGENTES,
                EstadosAsignacion.CANCELADA,
                idAsignacion,
                idTurno);
    }

    public int cancelActiveByTurno(Integer idTurno) {
        return jdbcTemplate.update(
                "update asignacion_turno a set estado = ? "
                        + "where a.id_turno = ? and " + ESTADOS_VIGENTES,
                EstadosAsignacion.CANCELADA,
                idTurno);
    }
}