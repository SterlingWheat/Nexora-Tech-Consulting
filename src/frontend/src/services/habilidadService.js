import { request } from './apiClient';

export const habilidadService = {
  // Catálogo de habilidades, usado para filtrar el calendario de disponibilidad
  listar() {
    return request('/habilidades', { mensajeError: 'Error al obtener las habilidades' });
  }
};
