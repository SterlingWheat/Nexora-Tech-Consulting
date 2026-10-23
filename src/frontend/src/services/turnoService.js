import { request } from './apiClient';

export const turnoService = {
  // Criterio 3: cada turno trae cuposTotales, cuposOcupados y cuposLibres
  obtenerTurnos() {
    return request('/turnos', { mensajeError: 'Error al obtener los turnos' });
  },

  obtenerTurno(id) {
    return request(`/turnos/${id}`, { mensajeError: 'Error al obtener el turno' });
  },

  // KAN-22: resuelve si el voluntario puede ser asignado al turno; si se cruza con otro turno suyo
  // lanza un Error cuyo mensaje indica con qué turno choca (para la pantalla de asignación, KAN-31)
  validarCruce(idVoluntario, idTurno) {
    return request(`/voluntarios/${idVoluntario}/turnos/${idTurno}/validacion-cruce`, {
      mensajeError: 'Error al validar el cruce de horarios',
    });
  },

  // Criterio 1 y 2: crea el turno o rechaza fecha pasada / horario inválido
  crearTurno(turno) {
    return request('/turnos', { method: 'POST', body: turno, mensajeError: 'Error al crear el turno' });
  }
};