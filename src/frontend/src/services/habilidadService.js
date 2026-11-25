import { request } from './apiClient';

export const habilidadService = {
  obtenerHabilidades() {
    return request('/admin/habilidades', { 
      mensajeError: 'Error al obtener el catálogo de habilidades.' 
    });
  },

  obtenerHabilidad(id) {
    return request(`/admin/habilidades/${id}`, { 
      mensajeError: 'Error al obtener los detalles de la habilidad.' 
    });
  },

  crearHabilidad(habilidad) {
    return request('/admin/habilidades', { 
      method: 'POST', 
      body: habilidad, 
      mensajeError: 'Error al crear la habilidad.' 
    });
  },

  actualizarHabilidad(id, habilidad) {
    return request(`/admin/habilidades/${id}`, { 
      method: 'PUT', 
      body: habilidad, 
      mensajeError: 'Error al actualizar la habilidad.' 
    });
  },

  eliminarHabilidad(id) {
    return request(`/admin/habilidades/${id}`, { 
      method: 'DELETE', 
      mensajeError: 'Error al eliminar la habilidad.' 
    });
  }
};