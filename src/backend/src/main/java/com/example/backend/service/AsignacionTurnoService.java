package com.example.backend.service;

import com.example.backend.model.TurnoAssignmentRecord;

import java.util.List;

public interface AsignacionTurnoService {

    /** Resultado de asignar: las filas creadas y los avisos no bloqueantes (p. ej. sin disponibilidad declarada). */
    record ResultadoAsignacionTurno(List<TurnoAssignmentRecord> asignaciones, List<String> advertencias) {}

    // KAN-31 Criterios 1, 2, 3 y 4: cupos, duplicados, disponibilidad (advertencia) y cruces (bloqueante)
    ResultadoAsignacionTurno asignarVoluntarios(Integer idTurno, List<Integer> idVoluntarios);

    List<TurnoAssignmentRecord> listarAsignaciones(Integer idTurno);

    void cancelarAsignacion(Integer idTurno, Long idAsignacion);
}
