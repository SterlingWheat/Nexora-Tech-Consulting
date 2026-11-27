package com.example.backend.service;

import com.example.backend.dto.ReporteTablaDTO;

// RF-47 (KAN-58): datos base de los reportes de voluntarios, tareas y turnos,
// listos para exportarse a PDF o Excel.
public interface ReporteService {

    ReporteTablaDTO reporteVoluntarios();

    ReporteTablaDTO reporteTareas();

    ReporteTablaDTO reporteTurnos();
}
