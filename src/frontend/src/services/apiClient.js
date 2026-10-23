import { authService } from './authService';

// Cliente HTTP compartido: un solo lugar para la URL base y el manejo de errores.
export const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export async function request(path, { method = 'GET', body, mensajeError = 'Error en la solicitud' } = {}) {
  // Inyección automática del token de autorización
  const opciones = { 
    method,
    headers: {
      ...authService.getAuthorizationHeader()
    }
  };

  if (body !== undefined) {
    opciones.headers['Content-Type'] = 'application/json';
    opciones.body = JSON.stringify(body);
  }

  const respuesta = await fetch(`${API_BASE}${path}`, opciones);

  // Manejo centralizado de sesión expirada
  if (respuesta.status === 401) {
    window.dispatchEvent(new Event('auth:expired'));
  }

  if (!respuesta.ok) {
    // El backend responde los errores (400, 404, 409) con un mensaje claro en texto plano
    const texto = await respuesta.text();
    throw new Error(texto || mensajeError);
  }

  // Prevenir fallos en respuestas exitosas vacías (ej. 204 No Content)
  const textResponse = await respuesta.text();
  return textResponse ? JSON.parse(textResponse) : null;
}