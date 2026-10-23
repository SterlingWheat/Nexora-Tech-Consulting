import { request } from './apiClient';

export const asignacionTareaService = {
  
  // HU2 Criterio 1 y 2: Asignar uno o más voluntarios a una tarea
  asignarVoluntarios(idTarea, idVoluntarios) {
    return request(`/tareas/${idTarea}/asignaciones`, {
      method: 'POST',
      body: { idVoluntarios },
      mensajeError: 'Error al asignar los voluntarios a la tarea.'
    });
  },

  // Obtener la lista de asignaciones de una tarea
  obtenerAsignaciones(idTarea) {
    return request(`/tareas/${idTarea}/asignaciones`, {
      mensajeError: 'Error al cargar las asignaciones de la tarea.'
    });
  }
};