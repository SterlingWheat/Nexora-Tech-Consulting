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
      <select id="categoria" v-model.number="formularioLocal.idCategoria" required class="tarea-select">
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
      <span aria-hidden="true">⚠️</span>
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
  // Se garantiza la estructura limpia antes de emitirla al componente padre
  const payloadEmitido = {
    titulo: formularioLocal.value.titulo.trim(),
    descripcion: formularioLocal.value.descripcion ? formularioLocal.value.descripcion.trim() : '',
    idCategoria: Number(formularioLocal.value.idCategoria)
  }
  emit('guardar', payloadEmitido)
}
</script>

<style scoped>
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
.tarea-campo input, .tarea-campo textarea, .tarea-select {
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
.tarea-campo input:focus, .tarea-campo textarea:focus, .tarea-select:focus {
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
  margin-top: 15px;
  border: none;
  background: transparent;
  color: var(--texto-suave);
  font-size: 12px;
  cursor: pointer;
}
.tarea-volver:hover {
  color: var(--verde-oscuro);
}
</style>