import { request } from './apiClient'

// RF-45/46 (KAN-56): tipos de reporte soportados y su descripción para la vista
export const TIPOS_REPORTE = [
  { valor: 'voluntarios', etiqueta: 'Voluntarios' },
  { valor: 'tareas', etiqueta: 'Tareas' },
  { valor: 'turnos', etiqueta: 'Turnos' }
]

export const reporteService = {
  async obtenerReporte(tipo) {
    return request(`/reportes/${tipo}`, { mensajeError: 'Error al obtener el reporte' })
  }
}
