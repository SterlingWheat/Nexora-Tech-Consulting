import { API_BASE_URL } from './apiConfig'
import { authService } from './authService'

const requestPerfil = async (path, mensajeError) => {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: authService.getAuthorizationHeader()
  })
  if (response.status === 401) {
    window.dispatchEvent(new Event('auth:expired'))
  }
  if (!response.ok) {
    throw new Error(response.status === 404 ? 'No se encontró el perfil solicitado.' : mensajeError)
  }
  return response.json()
}

export const perfilService = {
  obtenerMiPerfil() {
    return requestPerfil('/perfil/me', 'No se pudo cargar tu perfil.')
  },

  async listarUsuarios() {
    const usuarios = await requestPerfil('/usuarios', 'No se pudo cargar la lista de voluntarios.')
    return usuarios
  },

  obtenerPerfil(id) {
    return requestPerfil(`/usuarios/${encodeURIComponent(id)}`, 'No se pudo cargar el perfil.')
  }
}
