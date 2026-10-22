import { defineStore } from 'pinia';
import { tareaService } from '../services/tareaService';

export const useTareaStore = defineStore('tarea', {
  state: () => ({
    tareas: [],
    categorias: [],
    cargando: false,
    error: null,
  }),

  getters: {
    // Getter para buscar una tarea específica por su ID (útil para el formulario de edición)
    obtenerTareaPorId: (state) => (id) => {
      return state.tareas.find(t => t.idTarea === Number(id));
    }
  },

  actions: {
    async fetchTareas() {
      this.cargando = true;
      this.error = null;
      try {
        this.tareas = await tareaService.obtenerTareas();
      } catch (err) {
        this.error = err.message;
      } finally {
        this.cargando = false;
      }
    },

    // Criterio 4: Las categorías se seleccionan de un catálogo.
    async fetchCategorias() {
      try {
        this.categorias = await tareaService.obtenerCategorias();
      } catch (err) {
        console.error("Error cargando categorías:", err);
      }
    },

    // Criterio 1: Crear tarea
    async addTarea(tareaData) {
      this.cargando = true;
      this.error = null;
      try {
        const nuevaTarea = await tareaService.crearTarea(tareaData);
        this.tareas.push(nuevaTarea);
        return { exito: true };
      } catch (err) {
        this.error = err.message;
        return { exito: false, mensaje: err.message };
      } finally {
        this.cargando = false;
      }
    },

    // Criterio 2 y 3: Editar tarea
    async updateTarea(id, tareaData) {
      this.cargando = true;
      this.error = null;
      try {
        const tareaActualizada = await tareaService.editarTarea(id, tareaData);
        // Actualizamos la tarea en el estado local
        const index = this.tareas.findIndex(t => t.idTarea === Number(id));
        if (index !== -1) {
          this.tareas[index] = tareaActualizada;
        }
        return { exito: true };
      } catch (err) {
        this.error = err.message;
        return { exito: false, mensaje: err.message }; // Si está Finalizada/Cancelada, el mensaje vendrá del backend
      } finally {
        this.cargando = false;
      }
    }
  }
});