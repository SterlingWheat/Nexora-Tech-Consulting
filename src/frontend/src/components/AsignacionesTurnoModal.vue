<template>
  <div v-if="visible" class="modal-overlay" @click.self="cerrar">
    <section class="modal-contenido" role="dialog" aria-modal="true" aria-labelledby="turno-asignaciones-titulo">
      <header class="modal-encabezado">
        <div>
          <h2 id="turno-asignaciones-titulo">Asignaciones del turno</h2>
          <p>{{ turno?.actividad }}</p>
        </div>
        <button class="btn-cerrar" type="button" aria-label="Cerrar" @click="cerrar">&times;</button>
      </header>

      <div v-if="cargando" class="modal-mensaje">Cargando asignaciones...</div>
      
      <div v-else>
        <p v-if="error" class="modal-error" role="alert">{{ error }}</p>
        
        <!-- KAN-31 Criterio 3: avisos de disponibilidad -->
        <div v-if="advertencias.length" class="modal-aviso" role="status">
          <span aria-hidden="true">💡</span>
          <ul>
            <li v-for="a in advertencias" :key="a">{{ a }}</li>
          </ul>
        </div>

        <section v-if="turno?.estado !== 'CANCELADO' && !turnoIniciado" class="asignacion-nueva">
          <h3>Asignar voluntarios</h3>
          <p v-if="voluntariosDisponibles.length === 0" class="modal-mensaje">
            No hay voluntarios activos disponibles para este turno.
          </p>
          <div v-else class="lista-voluntarios">
            <label
              v-for="voluntario in voluntariosDisponibles"
              :key="voluntario.idVoluntario"
              class="voluntario-opcion"
              :class="{ seleccionado: seleccionados.includes(voluntario.idVoluntario) }"
            >
              <input v-model="seleccionados" type="checkbox" :value="voluntario.idVoluntario" />
              <span>{{ voluntario.nombre }} {{ voluntario.apellido }}</span>
            </label>
          </div>
          <button
            class="btn-guardar"
            type="button"
            :disabled="guardando || seleccionados.length === 0"
            @click="asignar"
          >
            {{ guardando ? 'Guardando...' : 'Asignar seleccionados' }}
          </button>
        </section>

        <section class="historial-asignaciones">
          <h3>Historial de asignaciones</h3>
          <p v-if="asignaciones.length === 0" class="modal-mensaje">
            Este turno todavía no tiene asignaciones.
          </p>
          <ul v-else>
            <li v-for="asignacion in asignaciones" :key="asignacion.idAsignacion">
              <span>
                <strong>{{ asignacion.nombreVoluntario }}</strong>
                <small :class="claseEstado(asignacion.estado)">{{ asignacion.estado || 'Pendiente' }}</small>
              </span>
              
              <div class="acciones-asignacion" v-if="turno?.estado !== 'CANCELADO' && estaVigente(asignacion)">
                <!-- HU2 Criterio 1: Confirmar asignación -->
                <button
                  v-if="(asignacion.estado || 'Pendiente') === 'Pendiente'"
                  class="btn-confirmar-asignacion"
                  type="button"
                  :disabled="guardando"
                  @click="confirmar(asignacion)"
                >
                  Confirmar
                </button>
                
                <!-- HU2 Criterio 3: Impide cancelar si el turno ya inició -->
                <button
                  class="btn-cancelar-asignacion"
                  type="button"
                  :disabled="guardando || turnoIniciado"
                  :title="turnoIniciado ? 'No se puede cancelar porque el turno ya inició' : 'Cancelar asignación'"
                  @click="cancelar(asignacion)"
                >
                  Cancelar
                </button>
              </div>
            </li>
          </ul>
        </section>
      </div>

      <footer class="modal-acciones">
        <button class="btn-cerrar-modal" type="button" @click="cerrar">Cerrar</button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { turnoService } from '../services/turnoService'
import { voluntarioService } from '../services/voluntarioService'

const props = defineProps({
  visible: { type: Boolean, default: false },
  turno: { type: Object, default: null }
})
const emit = defineEmits(['close', 'updated'])

const voluntarios = ref([])
const asignaciones = ref([])
const seleccionados = ref([])
const cargando = ref(false)
const guardando = ref(false)
const error = ref('')
const advertencias = ref([])

// HU2 Criterio 3: Determina si el turno ya ha iniciado según fecha y hora local
const turnoIniciado = computed(() => {
  if (!props.turno) return false
  const ahora = new Date()
  const fechaHoraTurno = new Date(`${props.turno.fecha}T${props.turno.horaInicio}`)
  return ahora >= fechaHoraTurno
})

const activosAsignados = computed(() => new Set(
  asignaciones.value
    .filter(estaVigente)
    .map(asignacion => asignacion.idVoluntario)
))

const voluntariosDisponibles = computed(() => voluntarios.value.filter(voluntario =>
  voluntario.activo && !activosAsignados.value.has(voluntario.idVoluntario)
))

// Se añade immediate: true para asegurar la carga independientemente de cómo se monte
watch(
  () => props.visible,
  (visible) => {
    if (visible) cargar()
  },
  { immediate: true }
)

function estaVigente(asignacion) {
  return !['cancelada', 'rechazada'].includes((asignacion.estado || '').toLowerCase())
}

function claseEstado(estado) {
  const e = (estado || 'pendiente').toLowerCase()
  if (e === 'confirmada') return 'estado-text-confirmado'
  if (e === 'cancelada' || e === 'rechazada') return 'estado-text-cancelado'
  return 'estado-text-pendiente'
}

async function cargar() {
  if (!props.turno) return
  cargando.value = true
  error.value = ''
  advertencias.value = []
  
  try {
    const [historial, listaVoluntarios] = await Promise.all([
      turnoService.obtenerAsignaciones(props.turno.idTurno),
      voluntarioService.obtenerVoluntarios()
    ])
    asignaciones.value = historial
    voluntarios.value = listaVoluntarios
    seleccionados.value = []
  } catch (err) {
    error.value = err.message
  } finally {
    cargando.value = false
  }
}

async function asignar() {
  if (seleccionados.value.length === 0 || !props.turno) return
  guardando.value = true
  error.value = ''
  
  try {
    const respuesta = await turnoService.asignarVoluntarios(props.turno.idTurno, seleccionados.value)
    seleccionados.value = []
    await cargar()
    advertencias.value = respuesta?.advertencias || []
    emit('updated')
  } catch (err) {
    error.value = err.message
  } finally {
    guardando.value = false
  }
}

async function confirmar(asignacion) {
  guardando.value = true
  error.value = ''
  try {
    await turnoService.confirmarAsignacion(props.turno.idTurno, asignacion.idAsignacion)
    await cargar()
    emit('updated')
  } catch (err) {
    error.value = err.message
  } finally {
    guardando.value = false
  }
}

async function cancelar(asignacion) {
  if (turnoIniciado.value) {
    error.value = 'No se puede cancelar porque el turno ya ha iniciado.'
    return
  }
  if (!window.confirm(`¿Cancelar la asignación de ${asignacion.nombreVoluntario}?`)) return
  
  guardando.value = true
  error.value = ''
  try {
    await turnoService.cancelarAsignacion(props.turno.idTurno, asignacion.idAsignacion)
    await cargar()
    emit('updated')
  } catch (err) {
    error.value = err.message
  } finally {
    guardando.value = false
  }
}

function cerrar() {
  if (!guardando.value) emit('close')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  background: rgba(36, 44, 37, 0.45);
}
.modal-contenido {
  width: min(620px, 100%);
  max-height: 90vh;
  overflow: auto;
  padding: 28px;
  border-radius: 20px;
  background: white;
  box-shadow: 0 18px 55px rgba(0, 0, 0, 0.16);
}
.modal-encabezado, .modal-acciones, .historial-asignaciones li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.modal-encabezado h2, .asignacion-nueva h3, .historial-asignaciones h3 {
  color: var(--texto);
}
.modal-encabezado p, .modal-mensaje {
  color: var(--texto-suave);
  font-size: 13px;
}
.btn-cerrar {
  border: 0;
  background: transparent;
  color: var(--texto-suave);
  font-size: 28px;
  cursor: pointer;
}
.asignacion-nueva, .historial-asignaciones {
  margin-top: 24px;
}
.lista-voluntarios, .historial-asignaciones ul {
  display: grid;
  gap: 8px;
  margin: 14px 0;
  padding: 0;
  list-style: none;
}
.voluntario-opcion, .historial-asignaciones li {
  padding: 11px 13px;
  border: 1px solid var(--crema);
  border-radius: 10px;
}
.voluntario-opcion {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.voluntario-opcion.seleccionado {
  border-color: var(--verde-oscuro);
  background: var(--crema-claro);
}
.historial-asignaciones li span {
  display: grid;
  gap: 4px;
}
.historial-asignaciones small {
  font-weight: 600;
}
.estado-text-pendiente { color: #8A5A36; }
.estado-text-confirmado { color: #50704B; }
.estado-text-cancelado { color: #A34D41; }

.modal-error {
  color: #a34d41;
  font-size: 13px;
}
.modal-aviso {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 14px 17px;
  margin-top: 10px;
  border-radius: 12px;
  font-size: 12px;
  line-height: 1.5;
  border: 1px solid var(--durazno);
  background: #FFF7EE;
  color: #8A5A36;
}
.modal-aviso ul {
  margin: 0;
  padding-left: 16px;
}
.acciones-asignacion {
  display: flex;
  gap: 8px;
}
.btn-guardar, .btn-cerrar-modal, .btn-cancelar-asignacion, .btn-confirmar-asignacion {
  padding: 9px 14px;
  border: 0;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 600;
}
.btn-guardar, .btn-cerrar-modal {
  background: var(--verde-oscuro);
  color: white;
}
.btn-confirmar-asignacion {
  background: #E8F2E5;
  color: #50704B;
}
.btn-confirmar-asignacion:hover:not(:disabled) {
  background: #D9E5D6;
}
.btn-cancelar-asignacion {
  background: #fff0eb;
  color: #a34d41;
}
.btn-cancelar-asignacion:hover:not(:disabled) {
  background: #FADBD3;
}
.btn-guardar:disabled, .btn-cancelar-asignacion:disabled, .btn-confirmar-asignacion:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.modal-acciones {
  justify-content: flex-end;
  margin-top: 24px;
}
</style>