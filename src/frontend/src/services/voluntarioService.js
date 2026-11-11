import { request } from './apiClient';

export const voluntarioService = {
  obtenerVoluntarios() {
    return request('/voluntarios', { mensajeError: 'Error al obtener los voluntarios' });
  },

  obtenerVoluntario(id) {
    return request(`/voluntarios/${id}/detalle`, { mensajeError: 'Error al obtener el voluntario' });
  },

  registrarVoluntario(voluntario) {
    return request('/voluntarios', { method: 'POST', body: voluntario, mensajeError: 'Error al registrar el voluntario' });
  },

  actualizarVoluntario(id, voluntario) {
    return request(`/voluntarios/${id}`, { method: 'PUT', body: voluntario, mensajeError: 'Error al actualizar el voluntario' });
  },

  habilitarAcceso(idVoluntario, password) {
    return request(`/voluntarios/${idVoluntario}/habilitar-acceso`, {
      method: 'POST',
      body: { password },
      mensajeError: 'Error al habilitar la cuenta de acceso.'
    });
  },

  activarVoluntario(id) {
    return request(`/voluntarios/${id}/activar`, { 
      method: 'PUT', 
      mensajeError: 'Error al activar el voluntario' 
    });
  },

  desactivarVoluntario(id) {
    return request(`/voluntarios/${id}/desactivar`, {
      method: 'PUT',
      mensajeError: 'Error al desactivar el voluntario'
    });
  },

  // KAN-27 Criterios 1, 2 y 3: búsqueda por texto, filtros de estado/disponibilidad y paginación
  buscarVoluntarios({ texto, activo, diaSemana, page = 0, size = 10 } = {}) {
    const query = new URLSearchParams({ page, size });
    if (texto) query.set('texto', texto);
    if (activo !== null && activo !== undefined) query.set('activo', activo);
    if (diaSemana) query.set('diaSemana', diaSemana);

    return request(`/voluntarios/filtrar?${query.toString()}`, {
      mensajeError: 'Error al buscar voluntarios'
    });
  }
};