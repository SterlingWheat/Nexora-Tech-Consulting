package com.example.backend.service;

import com.example.backend.dto.ReporteTablaDTO;

// RF-45/46/47 (KAN-56/58): reportes de voluntarios, tareas y turnos, con un resumen y el
// detalle tabular correspondiente, listos también para exportarse a PDF o Excel.
public interface ReporteService {

    ReporteTablaDTO reporteVoluntarios();

    ReporteTablaDTO reporteTareas();

    ReporteTablaDTO reporteTurnos();
}
