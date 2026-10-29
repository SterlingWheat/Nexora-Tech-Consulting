<template>
  <div v-if="visible" class="modal-overlay" @click.self="cerrar">
    <div class="modal-contenido">
      <div class="modal-encabezado">
        <h2>Registrar Avance</h2>
        <button class="btn-cerrar" @click="cerrar">&times;</button>
      </div>

      <p class="modal-descripcion">
        Actualiza el porcentaje de avance y a de una observaci n para la tarea <strong>{{ tarea?.titulo }}</strong>.
      </p>

      <div v-if="error" class="modal-error" role="alert">{{ error }}</div>

      <form class="modal-formulario" @submit.prevent="confirmarRegistro">
        <div class="modal-campo">
          <label for="avance">Porcentaje de avance ({{ avance }}%)</label>
          <div class="rango-contenedor">
            <input
              id="avance-range"
              type="range"
              min="0"
              max="100"
              v-model.number="avance"
              class="input-rango"
            />
            <input
              id="avance"
              type="number"
              min="0"
              max="100"
              v-model.number="avance"
              class="input-numero"
            />
          </div>
        </div>

        <div class="modal-campo">
          <label for="observacion">Observaci n (Opcional)</label>
          <textarea
            id="observacion"
            v-model="observacion"
            placeholder="Detalla el progreso o cualquier inconveniente..."
            rows="3"
            maxlength="500"
          ></textarea>
        </div>

        <div class="modal-acciones">
          <button type="button" class="btn-cancelar" @click="cerrar" :disabled="guardando">Cancelar</button>
          <button type="submit" class="btn-guardar" :disabled="guardando">
            {{ guardando ? 'Guardando...' : 'Registrar Avance' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
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

const emit = defineEmits(['close', 'avanceRegistrado'])

const avance = ref(0)
const observacion = ref('')
const guardando = ref(false)
const error = ref('')

watch(() => props.visible, (isVisible) => {
  if (isVisible) {
    avance.value = props.tarea?.avance || 0
    observacion.value = ''
    error.value = ''
  }
})

const cerrar = () => {
  if (!guardando.value) {
    emit('close')
  }
}

const confirmarRegistro = async () => {
  guardando.value = true
  error.value = ''
  try {
    await tareaService.registrarAvance(props.tarea.idTarea, {
      avance: avance.value,
      observacion: observacion.value.trim()
    })
    emit('avanceRegistrado')
    emit('close')
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
.modal-formulario {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 25px;
}
.modal-campo {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.modal-campo label {
  font-size: 12px;
  font-weight: 700;
  color: var(--texto);
}
.rango-contenedor {
  display: flex;
  align-items: center;
  gap: 15px;
}
.input-rango {
  flex-grow: 1;
  accent-color: var(--verde-oscuro);
}
.input-numero {
  width: 70px;
  padding: 8px 10px;
  border: 1px solid var(--crema);
  border-radius: 10px;
  background: var(--crema-claro);
  font-size: 13px;
  font-family: inherit;
  color: var(--texto);
  text-align: center;
  outline: none;
}
.input-numero:focus {
  border-color: var(--salvia);
}
.modal-campo textarea {
  padding: 12px 16px;
  border: 1px solid var(--crema);
  border-radius: 12px;
  background: var(--crema-claro);
  font-size: 13px;
  font-family: inherit;
  color: var(--texto);
  outline: none;
  resize: vertical;
}
.modal-campo textarea:focus {
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