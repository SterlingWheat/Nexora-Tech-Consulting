import { request } from './apiClient';

export const historialTareaService = {
  
  // HU3 Criterio 2: Obtener el registro histórico de quién cambió qué y cuándo en una tarea
  obtenerHistorial(idTarea) {
    return request(`/tareas/${idTarea}/historial`, {
      mensajeError: 'Error al obtener el historial de la tarea.'
    });
  }
  
};