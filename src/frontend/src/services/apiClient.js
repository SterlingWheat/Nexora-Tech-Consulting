// Cliente HTTP compartido: un solo lugar para la URL base y el manejo de errores.
// Los servicios nuevos deben construirse sobre esto en lugar de repetir fetch + manejo de errores.
export const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export async function request(path, { method = 'GET', body, mensajeError = 'Error en la solicitud' } = {}) {
  const opciones = { method };
  if (body !== undefined) {
    opciones.headers = { 'Content-Type': 'application/json' };
    opciones.body = JSON.stringify(body);
  }

  const respuesta = await fetch(`${API_BASE}${path}`, opciones);
  if (!respuesta.ok) {
    // El backend responde los errores (400, 404, 409) con un mensaje claro en texto plano
    const texto = await respuesta.text();
    throw new Error(texto || mensajeError);
  }
  return await respuesta.json();
}
