package com.example.backend.service;

import com.example.backend.model.TurnoAssignmentRecord;

import java.util.List;

public interface AsignacionTurnoService {

    /** Resultado de asignar: las filas creadas y los avisos no bloqueantes (p. ej. sin disponibilidad declarada). */
    record ResultadoAsignacionTurno(List<TurnoAssignmentRecord> asignaciones, List<String> advertencias) {}

    // KAN-31 Criterios 1, 2, 3 y 4: cupos, duplicados, disponibilidad (advertencia) y cruces (bloqueante)
    ResultadoAsignacionTurno asignarVoluntarios(Integer idTurno, List<Integer> idVoluntarios);

    List<TurnoAssignmentRecord> listarAsignaciones(Integer idTurno);

    // HU2 Criterio 1: Confirmar la participación de un voluntario
    void confirmarAsignacion(Integer idTurno, Long idAsignacion);

    // HU2 Criterio 2 y 3: Cancelar asignación (libera cupo) validando que el turno no haya iniciado
    void cancelarAsignacion(Integer idTurno, Long idAsignacion);
}