import { authService } from './authService'
import { TASK_API_URL } from './apiConfig'

const headersAutenticados = (headers = {}) => ({
  ...headers,
  ...authService.getAuthorizationHeader()
})

const comprobarRespuesta = async (respuesta, mensaje) => {
  if (respuesta.status === 401) {
    window.dispatchEvent(new Event('auth:expired'))
  }
  if (!respuesta.ok) {
    const errorText = await respuesta.text()
    throw new Error(errorText || mensaje)
  }
  return respuesta.json()
}

export const tareaService = {
  async obtenerTareas() {
    const respuesta = await fetch(TASK_API_URL, { headers: headersAutenticados() })
    return comprobarRespuesta(respuesta, 'Error al obtener las tareas')
  },
  
  // HU1: Paginación y Filtrado de tareas
  async filtrarTareas(filtros = {}, page = 0, size = 10) {
    const query = new URLSearchParams({ page, size })
    
    if (filtros.estado) query.set('estado', filtros.estado)
    if (filtros.idCategoria) query.set('idCategoria', filtros.idCategoria)
    if (filtros.idResponsable) query.set('idResponsable', filtros.idResponsable)
    if (filtros.fechaInicio) query.set('fechaInicio', filtros.fechaInicio)
    if (filtros.fechaFin) query.set('fechaFin', filtros.fechaFin)

    const respuesta = await fetch(`${TASK_API_URL}/filtrar?${query.toString()}`, { 
      headers: headersAutenticados() 
    })
    return comprobarRespuesta(respuesta, 'Error al filtrar las tareas')
  },
  
  async obtenerCategorias() {
    const respuesta = await fetch(`${TASK_API_URL}/categorias`, {
      headers: headersAutenticados()
    })
    return comprobarRespuesta(respuesta, 'Error al obtener las categorías')
  },
  
  async crearTarea(tarea) {
    const respuesta = await fetch(TASK_API_URL, {
      method: 'POST',
      headers: headersAutenticados({
        'Content-Type': 'application/json',
      }),
      body: JSON.stringify(tarea)
    })
    return comprobarRespuesta(respuesta, 'Error al crear la tarea')
  },
  
  async editarTarea(id, tarea) {
    const respuesta = await fetch(`${TASK_API_URL}/${id}`, {
      method: 'PUT',
      headers: headersAutenticados({
        'Content-Type': 'application/json',
      }),
      body: JSON.stringify(tarea)
    })
    return comprobarRespuesta(respuesta, 'Error al editar la tarea')
  },
  
  // HU3: Cambiar estado de tarea
  async cambiarEstado(id, nuevoEstado) {
    const respuesta = await fetch(`${TASK_API_URL}/${id}/estado`, {
      method: 'PATCH',
      headers: headersAutenticados({
        'Content-Type': 'application/json',
      }),
      body: JSON.stringify({ nuevoEstado })
    })
    return comprobarRespuesta(respuesta, 'Error al cambiar el estado de la tarea')
  },

  // HU: Registrar Avance de tarea
  async registrarAvance(id, avanceData) {
    const respuesta = await fetch(`${TASK_API_URL}/${id}/avance`, {
      method: 'PATCH',
      headers: headersAutenticados({
        'Content-Type': 'application/json',
      }),
      body: JSON.stringify(avanceData)
    })
    return comprobarRespuesta(respuesta, 'Error al registrar el avance de la tarea')
  }
}