const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/tareas';

export const tareaService = {
  // Obtiene todas las tareas
  async obtenerTareas() {
    const respuesta = await fetch(API_URL);
    if (!respuesta.ok) {
      throw new Error('Error al obtener las tareas');
    }
    return await respuesta.json();
  },

  // Criterio 4: Obtiene el catálogo de categorías
  async obtenerCategorias() {
    const respuesta = await fetch(`${API_URL}/categorias`);
    if (!respuesta.ok) {
      throw new Error('Error al obtener las categorías');
    }
    return await respuesta.json();
  },

  // Criterio 1: Crear tarea (El backend se encarga de ponerla en estado 'Pendiente')
  async crearTarea(tarea) {
    const respuesta = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(tarea),
    });
    
    if (!respuesta.ok) {
      const errorText = await respuesta.text();
      throw new Error(errorText || 'Error al crear la tarea');
    }
    return await respuesta.json();
  },

  // Criterio 2 y 3: Editar tarea
  async editarTarea(id, tarea) {
    const respuesta = await fetch(`${API_URL}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(tarea),
    });

    if (!respuesta.ok) {
      const errorText = await respuesta.text();
      // Si el status es 409 (Conflict), significa que la tarea está Finalizada o Cancelada (Criterio 3)
      throw new Error(errorText || 'Error al editar la tarea');
    }
    return await respuesta.json();
  }
};