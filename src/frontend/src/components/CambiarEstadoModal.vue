<template>
  <div v-if="visible" class="modal-overlay" @click.self="cerrar">
    <div class="modal-contenido">
      <div class="modal-encabezado">
        <h2>Cambiar Estado</h2>
        <button class="btn-cerrar" @click="cerrar">&times;</button>
      </div>
      
      <p class="modal-descripcion">
        Actualiza la situación de la tarea <strong>{{ tarea?.titulo }}</strong>. 
        El estado actual es <span class="estado-actual">{{ tarea?.estado }}</span>.
      </p>

      <div v-if="error" class="modal-error">{{ error }}</div>

      <div class="modal-formulario">
        <label for="nuevoEstado">Nuevo Estado</label>
        <select id="nuevoEstado" v-model="nuevoEstado">
          <option value="" disabled>Selecciona un estado...</option>
          <option v-for="estado in estadosDisponibles" :key="estado" :value="estado">
            {{ estado }}
          </option>
        </select>
      </div>

      <div class="modal-acciones">
        <button class="btn-cancelar" @click="cerrar" :disabled="guardando">Cancelar</button>
        <button 
          class="btn-guardar" 
          @click="confirmarCambio" 
          :disabled="guardando || !nuevoEstado || nuevoEstado === tarea?.estado"
        >
          {{ guardando ? 'Guardando...' : 'Confirmar Cambio' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { tareaService } from '../services/tareaService'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  tarea: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'estadoCambiado'])

const nuevoEstado = ref('')
const guardando = ref(false)
const error = ref('')

const todosLosEstados = ['Pendiente', 'Asignada', 'En progreso', 'Finalizada', 'Cancelada']

// HU3 Criterio 3: No permitir transiciones si ya está Finalizada o Cancelada
const estadosDisponibles = computed(() => {
  if (!props.tarea) return []
  if (['Finalizada', 'Cancelada'].includes(props.tarea.estado)) {
    return [] 
  }
  return todosLosEstados.filter(e => e !== props.tarea.estado)
})

watch(() => props.visible, (isVisible) => {
  if (isVisible) {
    nuevoEstado.value = ''
    error.value = ''
  }
})

const cerrar = () => {
  if (!guardando.value) {
    emit('close')
  }
}

const confirmarCambio = async () => {
  if (!nuevoEstado.value) return
  
  guardando.value = true
  error.value = ''
  try {
    // Se asume que tareaService.cambiarEstado fue implementado en la modificación de services/tareaService.js
    await tareaService.cambiarEstado(props.tarea.idTarea, nuevoEstado.value)
    emit('estadoCambiado')
    cerrar()
  } catch (err) {
    error.value = err.message
  } finally {
    guardando.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(81, 72, 62, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}
.modal-contenido {
  background: white;
  width: min(450px, 90%);
  border-radius: 24px;
  padding: 30px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.1);
}
.modal-encabezado {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.modal-encabezado h2 {
  margin: 0;
  font-family: "DM Serif Display", serif;
  font-size: 26px;
  color: var(--texto);
}
.btn-cerrar {
  background: transparent;
  border: none;
  font-size: 28px;
  line-height: 1;
  color: var(--texto-suave);
  cursor: pointer;
}
.btn-cerrar:hover {
  color: var(--texto);
}
.modal-descripcion {
  font-size: 13px;
  color: var(--texto-suave);
  margin-bottom: 20px;
  line-height: 1.5;
}
.estado-actual {
  font-weight: 700;
  color: var(--texto);
}
.modal-formulario {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 25px;
}
.modal-formulario label {
  font-size: 12px;
  font-weight: 700;
  color: var(--texto);
}
.modal-formulario select {
  padding: 12px 16px;
  border: 1px solid var(--crema);
  border-radius: 12px;
  background: var(--crema-claro);
  font-size: 13px;
  font-family: inherit;
  color: var(--texto);
  outline: none;
}
.modal-formulario select:focus {
  border-color: var(--salvia);
}
.modal-error {
  padding: 12px;
  background: #FFF0EB;
  color: #A34D41;
  border-radius: 10px;
  font-size: 12px;
  margin-bottom: 20px;
  border: 1px solid #E9B0A5;
}
.modal-acciones {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.btn-cancelar {
  padding: 12px 20px;
  background: transparent;
  border: none;
  color: var(--texto-suave);
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
}
.btn-cancelar:hover {
  color: var(--texto);
}
.btn-guardar {
  padding: 12px 24px;
  background: var(--verde-oscuro);
  color: white;
  border: none;
  border-radius: 30px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
}
.btn-guardar:hover:not(:disabled) {
  background: var(--salvia);
}
.btn-guardar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>