import { defineStore } from 'pinia';
import { tareaService } from '../services/tareaService';

export const useTareaStore = defineStore('tarea', {
  state: () => ({
    tareas: [],
    categorias: [],
    cargando: false,
    error: null,
    // HU1: Paginación state
    paginaActual: 0,
    totalPaginas: 0,
    totalElementos: 0
  }),
  getters: {
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
    
    // HU1: Paginación y Filtrado
    async fetchTareasFiltradas(filtros = {}, page = 0, size = 10) {
      this.cargando = true;
      this.error = null;
      try {
        const response = await tareaService.filtrarTareas(filtros, page, size);
        // Spring Data JPA Page format
        this.tareas = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
      } catch (err) {
        this.error = err.message;
      } finally {
        this.cargando = false;
      }
    },

    async fetchCategorias() {
      try {
        this.categorias = await tareaService.obtenerCategorias();
      } catch (err) {
        console.error("Error cargando categorías:", err);
      }
    },
    
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
    
    async updateTarea(id, tareaData) {
      this.cargando = true;
      this.error = null;
      try {
        const tareaActualizada = await tareaService.editarTarea(id, tareaData);
        const index = this.tareas.findIndex(t => t.idTarea === Number(id));
        if (index !== -1) {
          this.tareas[index] = tareaActualizada;
        }
        return { exito: true };
      } catch (err) {
        this.error = err.message;
        return { exito: false, mensaje: err.message }; 
      } finally {
        this.cargando = false;
      }
    },

    // HU3: Cambiar estado en el store
    async cambiarEstadoTarea(id, nuevoEstado) {
      this.cargando = true;
      this.error = null;
      try {
        const tareaActualizada = await tareaService.cambiarEstado(id, nuevoEstado);
        const index = this.tareas.findIndex(t => t.idTarea === Number(id));
        if (index !== -1) {
          this.tareas[index] = tareaActualizada;
        }
        return { exito: true };
      } catch (err) {
        this.error = err.message;
        return { exito: false, mensaje: err.message }; 
      } finally {
        this.cargando = false;
      }
    }
  }
});