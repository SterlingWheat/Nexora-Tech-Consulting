<template>
  <div v-if="visible" class="modal-overlay" @click.self="cerrar">
    <div class="modal-contenido">
      <div class="modal-encabezado">
        <h2>Asignar Voluntarios</h2>
        <button class="btn-cerrar" @click="cerrar">&times;</button>
      </div>
      
      <p class="modal-descripcion">
        Gestiona los voluntarios a cargo de la tarea <strong>{{ tarea?.titulo }}</strong>.
      </p>

      <section class="asignaciones-historial" aria-label="Asignaciones actuales e historial">
        <h3>Asignaciones actuales e historial</h3>
        
        <p v-if="cargandoAsignaciones" class="modal-mensaje">Cargando asignaciones...</p>
        <p v-else-if="errorAsignaciones" class="modal-error" role="alert">{{ errorAsignaciones }}</p>
        <p v-else-if="asignaciones.length === 0" class="modal-mensaje">
          Todavía no hay voluntarios asignados.
        </p>
        
        <ul v-else class="lista-asignaciones">
          <li v-for="asignacion in asignaciones" :key="asignacion.idAsignacion" :class="{ 'item-reemplazando': reemplazandoAsignacion?.idAsignacion === asignacion.idAsignacion }">
            <div class="asignacion-info">
              <strong>{{ asignacion.nombreVoluntario }}</strong>
              <span>{{ asignacion.estadoRespuesta || 'Asignado' }}</span>
            </div>
            <div class="acciones-asignacion" v-if="esCancelable(asignacion)">
              <button
                class="btn-reemplazar"
                type="button"
                :disabled="cancelandoId === asignacion.idAsignacion || reemplazandoAsignacion !== null"
                @click="iniciarReemplazo(asignacion)"
              >
                Reemplazar
              </button>
              <button
                class="btn-cancelar-asignacion"
                type="button"
                :disabled="cancelandoId === asignacion.idAsignacion || reemplazandoAsignacion !== null"
                @click="cancelarAsignacion(asignacion)"
              >
                {{ cancelandoId === asignacion.idAsignacion ? 'Cancelando...' : 'Cancelar' }}
              </button>
            </div>
          </li>
        </ul>
      </section>

      <div v-if="reemplazandoAsignacion" class="modo-reemplazo-banner" role="alert">
        <p>Selecciona un voluntario activo para reemplazar a <strong>{{ reemplazandoAsignacion.nombreVoluntario }}</strong>.</p>
        <button @click="cancelarReemplazo" class="btn-link-cancelar">Cancelar reemplazo</button>
      </div>

      <div v-if="cargando" class="modal-mensaje">Cargando voluntarios...</div>
      <div v-else-if="error" class="modal-error">{{ error }}</div>
      <div v-else-if="voluntariosActivos.length === 0" class="modal-mensaje">
        No hay voluntarios activos disponibles para asignar.
      </div>
      
      <div v-else class="lista-voluntarios" :class="{ 'seleccion-unica': reemplazandoAsignacion }">
        <label
            v-for="vol in voluntariosActivos"
            :key="vol.idVoluntario"
            class="voluntario-opcion"
           :class="{ seleccionado: seleccionados.includes(vol.idVoluntario), deshabilitado: esOpcionDeshabilitada(vol.idVoluntario) }"
        >
          <input
              type="checkbox"
              :value="vol.idVoluntario"
              v-model="seleccionados"
              :disabled="esOpcionDeshabilitada(vol.idVoluntario)"
           />
          <div class="voluntario-info">
            <span class="nombre">{{ vol.nombre }} {{ vol.apellido }}</span>
            <span class="doc">Doc: {{ vol.documento }}</span>
          </div>
        </label>
      </div>

      <div class="modal-acciones">
        <button class="btn-cancelar" @click="cerrar" :disabled="guardando || cancelandoId !== null">Cerrar</button>
        
        <button v-if="reemplazandoAsignacion"
           class="btn-guardar btn-confirmar-reemplazo" 
            @click="confirmarReemplazo" 
            :disabled="guardando || seleccionados.length === 0"
        >
          {{ guardando ? 'Reemplazando...' : 'Confirmar Reemplazo' }}
        </button>
        
        <button v-else
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
const asignaciones = ref([])
const seleccionados = ref([])

const cargando = ref(false)
const cargandoAsignaciones = ref(false)
const guardando = ref(false)
const cancelandoId = ref(null)

const error = ref('')
const errorAsignaciones = ref('')

const reemplazandoAsignacion = ref(null)

// Se filtran los voluntarios que están inactivos O que ya están asignados a esta tarea
const voluntariosActivos = computed(() => {
  const idsAsignados = asignaciones.value
    .filter(a => {
      const estado = (a.estadoRespuesta || '').toLowerCase()
      return estado !== 'cancelada' && estado !== 'rechazada'
    })
    .map(a => a.idVoluntario)

  return voluntarios.value.filter(v => v.activo && !idsAsignados.includes(v.idVoluntario))
})

watch(() => props.visible, async (isVisible) => {
  if (isVisible) {
    seleccionados.value = []
    error.value = ''
    reemplazandoAsignacion.value = null
    await Promise.all([cargarVoluntarios(), cargarAsignaciones()])
  }
})

// Controla que solo se seleccione un voluntario en modo reemplazo
watch(() => seleccionados.value, (newVals) => {
  if (reemplazandoAsignacion.value && newVals.length > 1) {
    seleccionados.value = [newVals[newVals.length - 1]]
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

const cargarAsignaciones = async () => {
  cargandoAsignaciones.value = true
  errorAsignaciones.value = ''
  try {
    asignaciones.value = await asignacionTareaService.obtenerAsignaciones(props.tarea.idTarea)
  } catch (err) {
    errorAsignaciones.value = err.message || 'Error al cargar las asignaciones de la tarea.'
  } finally {
    cargandoAsignaciones.value = false
  }
}

const esCancelable = (asignacion) => {
  return !['cancelada', 'rechazada'].includes((asignacion.estadoRespuesta || '').toLowerCase())
}

const esOpcionDeshabilitada = (idVoluntario) => {
  if (!reemplazandoAsignacion.value) return false
  return idVoluntario === reemplazandoAsignacion.value.idVoluntario
}

const cerrar = () => {
  if (!guardando.value && cancelandoId.value === null) {
    cancelarReemplazo()
    emit('close')
  }
}

// LOGICA DE CANCELACION DIRECTA
const cancelarAsignacion = async (asignacion) => {
  const confirmar = window.confirm(`¿Cancelar la asignación de ${asignacion.nombreVoluntario}?`)
  if (!confirmar) return

  cancelandoId.value = asignacion.idAsignacion
  errorAsignaciones.value = ''
  try {
    await asignacionTareaService.cancelarAsignacion(props.tarea.idTarea, asignacion.idAsignacion)
    await cargarAsignaciones()
    emit('asignado')
  } catch (err) {
    errorAsignaciones.value = err.message || 'Error al cancelar la asignación de la tarea.'
  } finally {
    cancelandoId.value = null
  }
}

// LOGICA DE ASIGNACION (Multiples)
const confirmarAsignacion = async () => {
  if (seleccionados.value.length === 0) return
  guardando.value = true
  error.value = ''
  try {
    await asignacionTareaService.asignarVoluntarios(props.tarea.idTarea, seleccionados.value)
    emit('asignado')
    emit('close') // Forzamos el cierre tras asignar con éxito
  } catch (err) {
    error.value = err.message
  } finally {
    guardando.value = false
  }
}

// LOGICA DE REEMPLAZO
const iniciarReemplazo = (asignacion) => {
  reemplazandoAsignacion.value = asignacion
  seleccionados.value = []
  error.value = ''
}

const cancelarReemplazo = () => {
  reemplazandoAsignacion.value = null
  seleccionados.value = []
  error.value = ''
}

const confirmarReemplazo = async () => {
  if (seleccionados.value.length === 0 || !reemplazandoAsignacion.value) return
  guardando.value = true
  error.value = ''
  try {
    await asignacionTareaService.reemplazarVoluntario(
      props.tarea.idTarea, 
      reemplazandoAsignacion.value.idAsignacion, 
      seleccionados.value[0]
    )
    await cargarAsignaciones()
    cancelarReemplazo()
    emit('asignado') // Refrescar la tabla en el backend
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
  max-height: 300px;
  padding-right: 5px;
  margin-bottom: 20px;
}
.asignaciones-historial {
  margin-bottom: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--crema);
}
.asignaciones-historial h3 {
  margin: 0 0 10px;
  color: var(--texto);
  font-size: 15px;
}
.lista-asignaciones {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 180px;
  overflow-y: auto;
  margin: 0;
  padding: 0;
  list-style: none;
}
.lista-asignaciones li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--crema);
  border-radius: 10px;
  transition: background 0.3s;
}
.lista-asignaciones li.item-reemplazando {
  background: #f1f5ec;
  border-color: var(--salvia-claro);
}
.asignacion-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  color: var(--texto);
  font-size: 12px;
}
.asignacion-info span {
  color: var(--texto-suave);
  font-size: 11px;
}
.acciones-asignacion {
  display: flex;
  gap: 8px;
}
.btn-cancelar-asignacion, .btn-reemplazar {
  border: none;
  background: transparent;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
}
.btn-cancelar-asignacion {
  color: #a34d41;
}
.btn-cancelar-asignacion:hover:not(:disabled) {
  background: #FFF0EB;
}
.btn-reemplazar {
  color: #4B6E91;
}
.btn-reemplazar:hover:not(:disabled) {
  background: #D9E5F2;
}
.btn-cancelar-asignacion:disabled, .btn-reemplazar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.modo-reemplazo-banner {
  background: #FFF7EE;
  border: 1px solid var(--durazno);
  color: #8A5A36;
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 16px;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.modo-reemplazo-banner p {
  margin: 0;
}
.btn-link-cancelar {
  background: none;
  border: none;
  color: #a34d41;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
  text-align: left;
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
.voluntario-opcion:hover:not(.deshabilitado) {
  background: var(--crema-claro);
}
.voluntario-opcion.seleccionado {
  border-color: var(--verde-oscuro);
  background: #f1f5ec;
}
.voluntario-opcion.deshabilitado {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f9f9f9;
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
.btn-confirmar-reemplazo {
  background: #4B6E91;
}
.btn-guardar:hover:not(:disabled) {
  opacity: 0.9;
}
.btn-guardar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>