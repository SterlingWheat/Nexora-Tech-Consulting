import { request } from './apiClient';

export const turnoService = {
  // Criterio 3: cada turno trae cuposTotales, cuposOcupados y cuposLibres
  obtenerTurnos() {
    return request('/turnos', { mensajeError: 'Error al obtener los turnos' });
  },

  obtenerTurno(id) {
    return request(`/turnos/${id}`, { mensajeError: 'Error al obtener el turno' });
  },

  // Criterio 1 y 2: crea el turno o rechaza fecha pasada / horario inválido
  crearTurno(turno) {
    return request('/turnos', { method: 'POST', body: turno, mensajeError: 'Error al crear el turno' });
  },
};
