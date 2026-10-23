<template>
  <div v-if="visible" class="modal-overlay" @click.self="cerrar">
    <div class="modal-contenido">
      <div class="modal-encabezado">
        <h2>Asignar Voluntarios</h2>
        <button class="btn-cerrar" @click="cerrar">&times;</button>
      </div>
      
      <p class="modal-descripcion">
        Selecciona los voluntarios activos que se harán cargo de la tarea <strong>{{ tarea?.titulo }}</strong>.
      </p>

      <div v-if="cargando" class="modal-mensaje">Cargando voluntarios...</div>
      <div v-else-if="error" class="modal-error">{{ error }}</div>
      <div v-else-if="voluntariosActivos.length === 0" class="modal-mensaje">
        No hay voluntarios activos disponibles para asignar.
      </div>
      
      <div v-else class="lista-voluntarios">
        <label 
          v-for="vol in voluntariosActivos" 
          :key="vol.idVoluntario" 
          class="voluntario-opcion"
          :class="{ seleccionado: seleccionados.includes(vol.idVoluntario) }"
        >
          <input 
            type="checkbox" 
            :value="vol.idVoluntario" 
            v-model="seleccionados" 
          />
          <div class="voluntario-info">
            <span class="nombre">{{ vol.nombre }} {{ vol.apellido }}</span>
            <span class="doc">Doc: {{ vol.documento }}</span>
          </div>
        </label>
      </div>

      <div class="modal-acciones">
        <button class="btn-cancelar" @click="cerrar" :disabled="guardando">Cancelar</button>
        <button 
          class="btn-guardar" 
          @click="confirmarAsignacion" 
          :disabled="guardando || seleccionados.length === 0"
        >
          {{ guardando ? 'Asignando...' : 'Confirmar Asignación' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { voluntarioService } from '../services/voluntarioService'
import { asignacionTareaService } from '../services/asignacionTareaService'

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

const emit = defineEmits(['close', 'asignado'])

const voluntarios = ref([])
const seleccionados = ref([])
const cargando = ref(false)
const guardando = ref(false)
const error = ref('')

const voluntariosActivos = computed(() => {
  return voluntarios.value.filter(v => v.activo)
})

watch(() => props.visible, async (isVisible) => {
  if (isVisible) {
    seleccionados.value = []
    error.value = ''
    await cargarVoluntarios()
  }
})

const cargarVoluntarios = async () => {
  cargando.value = true
  try {
    voluntarios.value = await voluntarioService.obtenerVoluntarios()
  } catch (err) {
    error.value = 'Error al cargar la lista de voluntarios: ' + err.message
  } finally {
    cargando.value = false
  }
}

const cerrar = () => {
  if (!guardando.value) {
    emit('close')
  }
}

const confirmarAsignacion = async () => {
  if (seleccionados.value.length === 0) return
  
  guardando.value = true
  error.value = ''
  try {
    await asignacionTareaService.asignarVoluntarios(props.tarea.idTarea, seleccionados.value)
    emit('asignado')
    emit('close') // Forzamos el cierre emitiendo el evento directamente
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
  width: min(500px, 90%);
  max-height: 90vh;
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
.lista-voluntarios {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  max-height: 350px;
  padding-right: 5px;
  margin-bottom: 20px;
}
.voluntario-opcion {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--crema);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.voluntario-opcion:hover {
  background: var(--crema-claro);
}
.voluntario-opcion.seleccionado {
  border-color: var(--verde-oscuro);
  background: #f1f5ec;
}
.voluntario-info {
  display: flex;
  flex-direction: column;
}
.voluntario-info .nombre {
  font-size: 14px;
  font-weight: 600;
  color: var(--texto);
}
.voluntario-info .doc {
  font-size: 11px;
  color: var(--texto-suave);
}
.modal-mensaje {
  text-align: center;
  padding: 30px 0;
  color: var(--texto-suave);
  font-size: 13px;
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
  margin-top: auto;
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