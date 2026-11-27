package com.example.backend.service;

import com.example.backend.dto.ReporteTablaDTO;

// RF-45/46 (KAN-56): reportes de voluntarios, tareas y turnos, con un resumen y el
// detalle tabular correspondiente.
public interface ReporteService {

    ReporteTablaDTO reporteVoluntarios();

    ReporteTablaDTO reporteTareas();

    ReporteTablaDTO reporteTurnos();
}
