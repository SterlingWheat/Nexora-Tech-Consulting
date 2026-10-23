import { API_BASE_URL } from './apiConfig'
import { authService } from './authService'

const requestUsuarios = async (path, { method = 'GET', body } = {}) => {
  const headers = authService.getAuthorizationHeader()
  if (body) {
    headers['Content-Type'] = 'application/json'
  }
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })

  if (response.status === 401) {
    window.dispatchEvent(new Event('auth:expired'))
  }

  const responseBody = await response.text()
  if (!response.ok) {
    let message = responseBody
    try {
      message = JSON.parse(responseBody).message || responseBody
    } catch {
      // Keep the server's plain-text error message.
    }
    throw new Error(message || 'No se pudo completar la operación de usuario.')
  }

  if (!responseBody) {
    return null
  }
  try {
    return JSON.parse(responseBody)
  } catch {
    return responseBody
  }
}

export const usuarioService = {
  listar(filtros = {}) {
    const query = new URLSearchParams()
    if (filtros.rol) query.set('rol', filtros.rol)
    if (filtros.activo !== '' && filtros.activo != null) query.set('activo', filtros.activo)
    const suffix = query.size ? `?${query.toString()}` : ''
    return requestUsuarios(`/usuarios${suffix}`)
  },

  crear(usuario) {
    return requestUsuarios('/usuarios', { method: 'POST', body: usuario })
  },

  actualizar(id, usuario) {
    return requestUsuarios(`/usuarios/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: usuario
    })
  },

  desactivar(id) {
    return requestUsuarios(`/usuarios/${encodeURIComponent(id)}/desactivar`, {
      method: 'PUT'
    })
  }
}
