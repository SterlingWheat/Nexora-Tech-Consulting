import { request } from './apiClient';

export const dashboardService = {
  // RF-44 (KAN-49) Criterio 1: voluntarios activos, tareas abiertas, turnos próximos y asignaciones pendientes
  obtenerMetricas() {
    return request('/dashboard/metricas', { mensajeError: 'Error al obtener las métricas del panel' });
  }
};
