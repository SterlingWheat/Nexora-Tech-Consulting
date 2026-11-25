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

  // HU2: Se añade 'habilidadId' a los parámetros de búsqueda
  buscarVoluntarios({ texto, activo, diaSemana, habilidadId, page = 0, size = 10 } = {}) {
    const query = new URLSearchParams({ page, size });
    if (texto) query.set('texto', texto);
    if (activo !== null && activo !== undefined) query.set('activo', activo);
    if (diaSemana) query.set('diaSemana', diaSemana);
    if (habilidadId) query.set('habilidadId', habilidadId);

    return request(`/voluntarios/filtrar?${query.toString()}`, {
      mensajeError: 'Error al buscar voluntarios'
    });
  },

  // HU1: Nuevo método para consultar el historial unificado de tareas y turnos
  obtenerHistorial(idVoluntario, fechaDesde, fechaHasta) {
    const query = new URLSearchParams();
    if (fechaDesde) query.set('desde', fechaDesde);
    if (fechaHasta) query.set('hasta', fechaHasta);
    
    const queryString = query.toString();
    const url = queryString 
      ? `/voluntarios/${idVoluntario}/historial?${queryString}` 
      : `/voluntarios/${idVoluntario}/historial`;
      
    return request(url, {
      mensajeError: 'Error al cargar el historial del voluntario.'
    });
  }
};