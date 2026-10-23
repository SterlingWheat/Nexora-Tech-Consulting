import { request } from './apiClient';

export const DIAS = [
  { valor: 'LUNES', etiqueta: 'Lunes' },
  { valor: 'MARTES', etiqueta: 'Martes' },
  { valor: 'MIERCOLES', etiqueta: 'Miércoles' },
  { valor: 'JUEVES', etiqueta: 'Jueves' },
  { valor: 'VIERNES', etiqueta: 'Viernes' },
  { valor: 'SABADO', etiqueta: 'Sábado' },
  { valor: 'DOMINGO', etiqueta: 'Domingo' }
];

export const disponibilidadService = {
  // Criterio 1: la disponibilidad queda en el perfil del voluntario
  obtener(idVoluntario) {
    return request(`/voluntarios/${idVoluntario}/disponibilidad`, { mensajeError: 'Error al obtener la disponibilidad' });
  },

  // Reemplaza toda la disponibilidad; la respuesta incluye advertencias si se unificaron franjas
  guardar(idVoluntario, franjas) {
    return request(`/voluntarios/${idVoluntario}/disponibilidad`, {
      method: 'PUT',
      body: { franjas },
      mensajeError: 'Error al guardar la disponibilidad',
    });
  }
};