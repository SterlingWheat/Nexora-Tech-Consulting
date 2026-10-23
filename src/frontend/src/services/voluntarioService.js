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
  
  // Nuevo método
  habilitarAcceso(idVoluntario, password) {
    return request(`/voluntarios/${idVoluntario}/habilitar-acceso`, {
      method: 'POST',
      body: { password },
      mensajeError: 'Error al habilitar la cuenta de acceso.'
    });
  }
};