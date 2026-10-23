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
  }
}