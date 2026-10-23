const API_URL = import.meta.env.VITE_API_VOLUNTARIOS_URL || 'http://localhost:8080/api/voluntarios';

async function manejarRespuesta(respuesta, mensajePorDefecto) {
  if (!respuesta.ok) {
    const errorText = await respuesta.text();
    // 400: validaciones, 409: documento o correo duplicado (el backend envía un mensaje claro)
    throw new Error(errorText || mensajePorDefecto);
  }
  return await respuesta.json();
}

export const voluntarioService = {
  async obtenerVoluntarios() {
    return manejarRespuesta(await fetch(API_URL), 'Error al obtener los voluntarios');
  },

  async obtenerVoluntario(id) {
    return manejarRespuesta(await fetch(`${API_URL}/${id}`), 'Error al obtener el voluntario');
  },

  // Criterio 1 y 2: registrar voluntario (activo por defecto, rechaza duplicados)
  async registrarVoluntario(voluntario) {
    const respuesta = await fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(voluntario),
    });
    return manejarRespuesta(respuesta, 'Error al registrar el voluntario');
  },

  // Criterio 3: actualizar datos
  async actualizarVoluntario(id, voluntario) {
    const respuesta = await fetch(`${API_URL}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(voluntario),
    });
    return manejarRespuesta(respuesta, 'Error al actualizar el voluntario');
  },
};
