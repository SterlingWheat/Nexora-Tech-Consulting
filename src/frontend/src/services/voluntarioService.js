import { request } from './apiClient';

export const voluntarioService = {
  obtenerVoluntarios() {
    return request('/voluntarios', { mensajeError: 'Error al obtener los voluntarios' });
  },

  obtenerVoluntario(id) {
    return request(`/voluntarios/${id}`, { mensajeError: 'Error al obtener el voluntario' });
  },

  // Registrar voluntario (activo por defecto, rechaza documento o correo duplicado)
  registrarVoluntario(voluntario) {
    return request('/voluntarios', { method: 'POST', body: voluntario, mensajeError: 'Error al registrar el voluntario' });
  },

  actualizarVoluntario(id, voluntario) {
    return request(`/voluntarios/${id}`, { method: 'PUT', body: voluntario, mensajeError: 'Error al actualizar el voluntario' });
  },
};
