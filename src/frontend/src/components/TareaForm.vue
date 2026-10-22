<template>
  <form class="tarea-formulario" @submit.prevent="handleSubmit">
    <div class="tarea-campo">
      <label for="titulo">Título de la tarea *</label>
      <input 
        id="titulo" 
        type="text" 
        v-model="formularioLocal.titulo" 
        placeholder="Ej: Clasificar alimentos" 
        required 
      />
    </div>

    <div class="tarea-campo">
      <label for="categoria">Categoría *</label>
      <select id="categoria" v-model="formularioLocal.idCategoria" required class="tarea-select">
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
        v-model="formularioLocal.descripcion" 
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

    <button type="button" class="tarea-volver" @click="$emit('cancelar')">
      ← Volver a la lista
    </button>
  </form>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  tareaInicial: {
    type: Object,
    default: () => ({ titulo: '', descripcion: '', idCategoria: '' })
  },
  categorias: {
    type: Array,
    required: true,
    default: () => []
  },
  esEdicion: {
    type: Boolean,
    default: false
  },
  cargando: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['guardar', 'cancelar'])

const formularioLocal = ref({ ...props.tareaInicial })

watch(() => props.tareaInicial, (nuevoValor) => {
  formularioLocal.value = { ...nuevoValor }
}, { deep: true })

const handleSubmit = () => {
  emit('guardar', formularioLocal.value)
}
</script>

<style scoped>
.tarea-formulario {
  display: flex; /*[cite: 3] */
  flex-direction: column; /*[cite: 3] */
  gap: 23px; /*[cite: 3] */
  margin-top: 35px; /*[cite: 3] */
}

.tarea-campo {
  display: flex; /*[cite: 3] */
  flex-direction: column; /*[cite: 3] */
  gap: 9px; /*[cite: 3] */
}

.tarea-campo label {
  color: var(--texto); /*[cite: 3] */
  font-size: 12px; /*[cite: 3] */
  font-weight: 700; /*[cite: 3] */
}

.tarea-campo input,
.tarea-campo textarea,
.tarea-select {
  width: 100%; /*[cite: 3] */
  padding: 15px 17px; /*[cite: 3] */
  background: var(--crema-claro); /*[cite: 3] */
  border: 1px solid var(--crema); /*[cite: 3] */
  border-radius: 12px; /*[cite: 3] */
  outline: none; /*[cite: 3] */
  color: var(--texto); /*[cite: 3] */
  font-size: 13px; /*[cite: 3] */
  font-family: inherit;
  transition: border-color 0.2s; /*[cite: 3] */
  resize: vertical;
}

.tarea-campo input:focus,
.tarea-campo textarea:focus,
.tarea-select:focus {
  border-color: var(--salvia); /*[cite: 3] */
}

.tarea-boton {
  width: 100%; /*[cite: 3] */
  padding: 16px 24px; /*[cite: 3] */
  border: none; /*[cite: 3] */
  border-radius: 30px; /*[cite: 3] */
  background: var(--verde-oscuro); /*[cite: 3] */
  color: white; /*[cite: 3] */
  font-size: 13px; /*[cite: 3] */
  font-weight: 600; /*[cite: 3] */
  cursor: pointer; /*[cite: 3] */
  transition: transform 0.2s, background 0.2s; /*[cite: 3] */
}

.tarea-boton:hover {
  transform: translateY(-2px); /*[cite: 3] */
  background: var(--salvia); /*[cite: 3] */
}

.tarea-boton:disabled {
  opacity: 0.6; /*[cite: 3] */
  cursor: not-allowed; /*[cite: 3] */
}

.tarea-error {
  display: flex; /*[cite: 3] */
  align-items: center; /*[cite: 3] */
  gap: 11px; /*[cite: 3] */
  padding: 14px 17px; /*[cite: 3] */
  border: 1px solid #E9B0A5; /*[cite: 3] */
  border-radius: 12px; /*[cite: 3] */
  background: #FFF0EB; /*[cite: 3] */
  color: #A34D41; /*[cite: 3] */
  font-size: 12px; /*[cite: 3] */
  line-height: 1.5; /*[cite: 3] */
}

.tarea-volver {
  align-self: center; /*[cite: 3] */
  margin-top: 15px; /*[cite: 3] */
  border: none; /*[cite: 3] */
  background: transparent; /*[cite: 3] */
  color: var(--texto-suave); /*[cite: 3] */
  font-size: 12px; /*[cite: 3] */
  cursor: pointer;
}

.tarea-volver:hover {
  color: var(--verde-oscuro); /*[cite: 3] */
}
</style>