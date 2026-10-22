<template>
  <section class="tarea-form">
    <div class="tarea-contenedor">
      <div class="tarea-formulario-contenedor">
        <span class="tarea-etiqueta">{{ esEdicion ? 'MODIFICAR REGISTRO' : 'NUEVO REGISTRO' }}</span>
        <h1>
          {{ esEdicion ? 'Editar' : 'Crear' }} 
          <em>tarea.</em>
        </h1>
        <p class="tarea-descripcion">
          Completa los datos de la tarea operativa. Las tareas nuevas inician en estado Pendiente.
        </p>

        <form class="tarea-formulario" @submit.prevent="guardarTarea">
          
          <div class="tarea-campo">
            <label for="titulo">Título de la tarea *</label>
            <input 
              id="titulo" 
              type="text" 
              v-model="formulario.titulo" 
              placeholder="Ej: Clasificar alimentos" 
              required 
            />
          </div>

          <div class="tarea-campo">
            <label for="categoria">Categoría *</label>
            <select id="categoria" v-model.number="formulario.idCategoria" required class="tarea-select">
              <option value="" disabled>Selecciona una categoría</option>
              <option v-for="cat in categorias" :key="cat.idCategoria" :value="cat.idCategoria">
                {{ cat.nombre }}
              </option>
            </select>
          </div>

          <div class="tarea-campo">
            <label for="descripcion">Descripción</label>
            <textarea 
              id="descripcion" 
              v-model="formulario.descripcion" 
              placeholder="Detalles sobre lo que se debe hacer..."
              rows="4"
            ></textarea>
          </div>

          <div v-if="error" class="tarea-error" role="alert">
            <span aria-hidden="true">⚠</span>
            {{ error }}
          </div>

          <button type="submit" class="tarea-boton" :disabled="cargando">
            {{ cargando ? 'Guardando...' : (esEdicion ? 'Guardar Cambios' : 'Crear Tarea') }}
          </button>
        </form>

        <button class="tarea-volver" @click="volver">
          ← Volver a la lista
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { tareaService } from '../services/tareaService'

const router = useRouter()
const route = useRoute()

const esEdicion = ref(!!route.params.id)
const cargando = ref(false)
const error = ref('')
const categorias = ref([])

const formulario = ref({
  titulo: '',
  descripcion: '',
  idCategoria: ''
})

onMounted(async () => {
  try {
    // Criterio 4: Carga del catálogo real desde Neon DB
    categorias.value = await tareaService.obtenerCategorias()

    if (esEdicion.value) {
      const tareas = await tareaService.obtenerTareas()
      const tareaAEditar = tareas.find(t => t.idTarea === Number(route.params.id))
      if (tareaAEditar) {
        formulario.value = {
          titulo: tareaAEditar.titulo,
          descripcion: tareaAEditar.descripcion || '',
          idCategoria: tareaAEditar.idCategoria
        }
      }
    }
  } catch (err) {
    error.value = 'Error al cargar los datos: ' + err.message
  }
})

const guardarTarea = async () => {
  error.value = ''
  cargando.value = true

  try {
    const payload = {
      titulo: formulario.value.titulo.trim(),
      descripcion: formulario.value.descripcion ? formulario.value.descripcion.trim() : '',
      idCategoria: Number(formulario.value.idCategoria)
    }

    if (esEdicion.value) {
      await tareaService.editarTarea(route.params.id, payload)
    } else {
      await tareaService.crearTarea(payload)
    }

    router.push('/tareas')
  } catch (err) {
    error.value = err.message
  } finally {
    cargando.value = false
  }
}

const volver = () => {
  router.push('/tareas')
}
</script>

<style scoped>
.tarea-form {
  min-height: calc(100vh - 94px);
  padding: 60px 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--crema-claro);
}

.tarea-contenedor {
  width: min(600px, 100%);
  background: white;
  border-radius: 30px;
  box-shadow: 0 20px 60px rgba(81, 72, 62, 0.07);
  overflow: hidden;
}

.tarea-formulario-contenedor {
  padding: 65px min(9%, 75px);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.tarea-etiqueta {
  font-size: 10px;
  letter-spacing: 2.5px;
  font-weight: 700;
  color: var(--verde-oscuro);
}

.tarea-formulario-contenedor h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-size: clamp(34px, 3.5vw, 48px);
  font-weight: 400;
  line-height: 1.2;
  color: var(--texto);
}

.tarea-formulario-contenedor h1 em {
  color: var(--verde-oscuro);
}

.tarea-descripcion {
  color: var(--texto-suave);
  font-size: 13px;
  line-height: 1.8;
}

.tarea-formulario {
  display: flex;
  flex-direction: column;
  gap: 23px;
  margin-top: 35px;
}

.tarea-campo {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.tarea-campo label {
  color: var(--texto);
  font-size: 12px;
  font-weight: 700;
}

.tarea-campo input,
.tarea-campo textarea,
.tarea-select {
  width: 100%;
  padding: 15px 17px;
  background: var(--crema-claro);
  border: 1px solid var(--crema);
  border-radius: 12px;
  outline: none;
  color: var(--texto);
  font-size: 13px;
  font-family: inherit;
  transition: border-color 0.2s;
  resize: vertical;
}

.tarea-campo input:focus,
.tarea-campo textarea:focus,
.tarea-select:focus {
  border-color: var(--salvia);
}

.tarea-boton {
  width: 100%;
  padding: 16px 24px;
  border: none;
  border-radius: 30px;
  background: var(--verde-oscuro);
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, background 0.2s;
}

.tarea-boton:hover {
  transform: translateY(-2px);
  background: var(--salvia);
}

.tarea-boton:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.tarea-error {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 14px 17px;
  border: 1px solid #E9B0A5;
  border-radius: 12px;
  background: #FFF0EB;
  color: #A34D41;
  font-size: 12px;
  line-height: 1.5;
}

.tarea-volver {
  align-self: center;
  margin-top: 28px;
  border: none;
  background: transparent;
  color: var(--texto-suave);
  font-size: 12px;
  cursor: pointer;
}

.tarea-volver:hover {
  color: var(--verde-oscuro);
}

@media (max-width: 750px) {
  .tarea-formulario-contenedor { padding: 42px 28px; }
}
</style>