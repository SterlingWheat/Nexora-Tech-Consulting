package com.example.backend.service;

import com.example.backend.model.HistorialTurno;
import java.util.List;

public interface HistorialTurnoService {
    
    // HU1 Criterio 3: Registra quién hizo la acción, cuándo y el detalle del cambio en el turno
    HistorialTurno registrarAccion(Integer idTurno, Integer idUsuario, String detalle);
    
    List<HistorialTurno> obtenerHistorialPorTurno(Integer idTurno);
}