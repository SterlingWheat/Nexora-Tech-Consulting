import { API_BASE_URL } from './apiConfig'

const API_URL = `${API_BASE_URL}/auth/password-reset`

async function post(path, payload) {
  const response = await fetch(`${API_URL}/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  const responseText = await response.text()
  let body

  try {
    body = responseText ? JSON.parse(responseText) : {}
  } catch {
    body = { message: responseText }
  }

  if (!response.ok) {
    throw new Error(body.message || 'No se pudo completar la solicitud.')
  }

  return body
}

export const passwordResetService = {
  solicitar(email) {
    return post('request', { email })
  },

  validar(token) {
    return post('validate', { token })
  },

  restablecer(token, newPassword, confirmPassword) {
    return post('confirm', { token, newPassword, confirmPassword })
  }
}
