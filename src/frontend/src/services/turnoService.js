import { request } from './apiClient';

export const turnoService = {
  // Criterio 3: cada turno trae cuposTotales, cuposOcupados y cuposLibres
  obtenerTurnos() {
    return request('/turnos', { mensajeError: 'Error al obtener los turnos' });
  },
  
  obtenerTurno(id) {
    return request(`/turnos/${id}`, { mensajeError: 'Error al obtener el turno' });
  },
  
  actualizarTurno(id, turno) {
    return request(`/turnos/${id}`, {
      method: 'PUT',
      body: turno,
      mensajeError: 'Error al actualizar el turno',
    });
  },
  
  cancelarTurno(id) {
    return request(`/turnos/${id}/cancelar`, {
      method: 'POST',
      mensajeError: 'Error al cancelar el turno',
    });
  },
  
  asignarVoluntarios(idTurno, idVoluntarios) {
    return request(`/turnos/${idTurno}/asignaciones`, {
      method: 'POST',
      body: { idVoluntarios },
      mensajeError: 'Error al asignar los voluntarios al turno',
    });
  },
  
  obtenerAsignaciones(idTurno) {
    return request(`/turnos/${idTurno}/asignaciones`, {
      mensajeError: 'Error al consultar el historial de asignaciones',
    });
  },

  // HU2 Criterio 1: Confirmar asignación
  confirmarAsignacion(idTurno, idAsignacion) {
    return request(`/turnos/${idTurno}/asignaciones/${idAsignacion}/confirmar`, {
      method: 'PATCH',
      mensajeError: 'Error al confirmar la asignación del voluntario'
    });
  },
  
  // HU2 Criterios 2 y 3: Cancelar asignación
  cancelarAsignacion(idTurno, idAsignacion) {
    return request(`/turnos/${idTurno}/asignaciones/${idAsignacion}/cancelar`, {
      method: 'POST',
      mensajeError: 'Error al cancelar la asignación',
    });
  },
  
  // KAN-22: resuelve si el voluntario puede ser asignado al turno
  validarCruce(idVoluntario, idTurno) {
    return request(`/voluntarios/${idVoluntario}/turnos/${idTurno}/validacion-cruce`, {
      mensajeError: 'Error al validar el cruce de horarios',
    });
  },
  
  // Criterio 1 y 2: crea el turno o rechaza fecha pasada / horario inválido
  crearTurno(turno) {
    return request('/turnos', { method: 'POST', body: turno, mensajeError: 'Error al crear el turno' });
  },

  // RF-30 (KAN-53) Criterios 1 y 2: turnos por rango de fechas (día/semana/mes) con filtros opcionales
  obtenerCalendario({ desde, hasta, actividad, idVoluntario } = {}) {
    const query = new URLSearchParams({ desde, hasta });
    if (actividad) query.set('actividad', actividad);
    if (idVoluntario) query.set('idVoluntario', idVoluntario);

    return request(`/turnos/calendario?${query.toString()}`, {
      mensajeError: 'Error al obtener el calendario de turnos'
    });
  }
};