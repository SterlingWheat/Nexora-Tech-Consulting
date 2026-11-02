package com.example.backend.service;

import com.example.backend.model.TurnoAssignmentRecord;

import java.util.List;

public interface AsignacionTurnoService {

    List<TurnoAssignmentRecord> asignarVoluntarios(Integer idTurno, List<Integer> idVoluntarios);

    List<TurnoAssignmentRecord> listarAsignaciones(Integer idTurno);

    void cancelarAsignacion(Integer idTurno, Long idAsignacion);
}
