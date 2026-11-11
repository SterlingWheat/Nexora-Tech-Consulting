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

        <!-- KAN-31 Criterio 3: avisos cuando un voluntario asignado no declaró disponibilidad para esta franja -->
        <div v-if="advertencias.length" class="modal-aviso" role="status">
          <span aria-hidden="true">ℹ</span>
          <ul>
            <li v-for="a in advertencias" :key="a">{{ a }}</li>
          </ul>
        </div>

        <section v-if="turno?.estado !== 'CANCELADO'" class="asignacion-nueva">
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
                <small>{{ asignacion.estado || 'Pendiente' }}</small>
              </span>
              <button
                v-if="turno?.estado !== 'CANCELADO' && estaVigente(asignacion)"
                class="btn-cancelar-asignacion"
                type="button"
                :disabled="guardando"
                @click="cancelar(asignacion)"
              >
                Cancelar asignación
              </button>
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

const activosAsignados = computed(() => new Set(
  asignaciones.value
    .filter(estaVigente)
    .map(asignacion => asignacion.idVoluntario)
))

const voluntariosDisponibles = computed(() => voluntarios.value.filter(voluntario =>
  voluntario.activo && !activosAsignados.value.has(voluntario.idVoluntario)
))

watch(() => props.visible, visible => {
  if (visible) cargar()
})

function estaVigente(asignacion) {
  return !['cancelada', 'rechazada'].includes((asignacion.estado || '').toLowerCase())
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
    // KAN-31 Criterio 3: el aviso de disponibilidad se muestra tras el refresco (cargar() lo limpia)
    advertencias.value = respuesta?.advertencias || []
    emit('updated')
  } catch (err) {
    error.value = err.message
  } finally {
    guardando.value = false
  }
}

async function cancelar(asignacion) {
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

.modal-encabezado,
.modal-acciones,
.historial-asignaciones li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.modal-encabezado h2,
.asignacion-nueva h3,
.historial-asignaciones h3 {
  color: var(--texto);
}

.modal-encabezado p,
.modal-mensaje {
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

.asignacion-nueva,
.historial-asignaciones {
  margin-top: 24px;
}

.lista-voluntarios,
.historial-asignaciones ul {
  display: grid;
  gap: 8px;
  margin: 14px 0;
  padding: 0;
  list-style: none;
}

.voluntario-opcion,
.historial-asignaciones li {
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
  color: var(--texto-suave);
}

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

.btn-guardar,
.btn-cerrar-modal,
.btn-cancelar-asignacion {
  padding: 9px 14px;
  border: 0;
  border-radius: 20px;
  cursor: pointer;
}

.btn-guardar,
.btn-cerrar-modal {
  background: var(--verde-oscuro);
  color: white;
}

.btn-cancelar-asignacion {
  background: #fff0eb;
  color: #a34d41;
}

.btn-guardar:disabled,
.btn-cancelar-asignacion:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.modal-acciones {
  justify-content: flex-end;
  margin-top: 24px;
}
</style>
