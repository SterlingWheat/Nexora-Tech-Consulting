import { API_BASE_URL } from './apiConfig'

const API_URL = `${API_BASE_URL}/auth/login`

export const authService = {
  async login(email, password) {
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    })
    const body = await response.json()

    if (!response.ok) {
      throw new Error(body.message || 'No se pudo iniciar sesión.')
    }
    if (!body.token || !body.rol) {
      throw new Error('El servidor devolvió una respuesta de inicio de sesión inválida.')
    }

    return body
  },

  getAuthorizationHeader() {
    const token = sessionStorage.getItem('authToken')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }
}
