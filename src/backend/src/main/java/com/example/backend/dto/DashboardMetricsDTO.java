package com.example.backend.dto;

import java.util.List;

// RF-44 (KAN-49): indicadores clave del panel administrativo
public record DashboardMetricsDTO(
        Long voluntariosActivos,
        Long voluntariosInactivos,
        Long tareasAbiertas,
        List<ConteoPorEstadoDTO> tareasPorEstado,
        Long turnosProximos,
        Long asignacionesPendientes) {
}
