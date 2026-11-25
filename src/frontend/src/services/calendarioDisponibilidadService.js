import { request } from './apiClient';

export const calendarioDisponibilidadService = {
  // RF-23 Criterios 1 y 2: franjas por voluntario dentro de un rango de fechas, con filtro opcional
  obtenerCalendario({ desde, hasta, idVoluntario, idHabilidad } = {}) {
    const query = new URLSearchParams({ desde, hasta });
    if (idVoluntario) query.set('idVoluntario', idVoluntario);
    if (idHabilidad) query.set('idHabilidad', idHabilidad);

    return request(`/disponibilidad/calendario?${query.toString()}`, {
      mensajeError: 'Error al obtener el calendario de disponibilidad'
    });
  }
};
