package com.example.backend.service;

import com.example.backend.dto.DashboardMetricsDTO;

public interface DashboardService {

    // RF-44 (KAN-49) Criterio 1: voluntarios activos, tareas abiertas, turnos próximos y asignaciones pendientes
    DashboardMetricsDTO obtenerMetricas();
}
