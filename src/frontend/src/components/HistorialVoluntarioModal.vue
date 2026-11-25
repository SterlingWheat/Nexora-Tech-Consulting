<template>
  <div v-if="visible" class="modal-overlay" @click.self="cerrarModal">
    <div class="modal-contenido modal-ancho">
      <div class="modal-header">
        <div>
          <h2>Historial de Participación</h2>
          <p>
            Viendo las tareas y turnos de <strong>{{ voluntario?.nombre }} {{ voluntario?.apellido }}</strong>
          </p>
        </div>
        <button class="btn-cerrar" @click="cerrarModal" aria-label="Cerrar modal">&times;</button>
      </div>

      <div class="historial-filtros">
        <div class="filtro-fecha">
          <label for="fechaDesde">Desde:</label>
          <input type="date" id="fechaDesde" v-model="filtroDesde" />
        </div>
        <div class="filtro-fecha">
          <label for="fechaHasta">Hasta:</label>
          <input type="date" id="fechaHasta" v-model="filtroHasta" />
        </div>
        <button class="btn-limpiar" @click="limpiarFiltros" v-if="filtroDesde || filtroHasta">
          Limpiar filtros
        </button>
      </div>
      
      <div v-if="cargando" class="modal-mensaje">Cargando historial...</div>
      <div v-else-if="error" class="modal-mensaje modal-error-msg" role="alert">{{ error }}</div>
      
      <div v-else-if="historial.length === 0" class="modal-mensaje">
        No hay registros de participación para el periodo seleccionado.
      </div>
      
      <div v-else class="historial-lista">
        <div v-for="item in historial" :key="item.tipo + item.participacionId" class="historial-item">
          <div class="item-icono" :class="item.tipo === 'TAREA' ? 'icono-tarea' : 'icono-turno'">
            {{ item.tipo === 'TAREA' ? '📋' : '🕒' }}
          </div>
          <div class="item-detalles">
            <h4>{{ item.nombre }}</h4>
            <div class="item-meta">
              <span class="meta-badge" :class="item.tipo === 'TAREA' ? 'badge-tarea' : 'badge-turno'">
                {{ item.tipo }}
              </span>
              <span class="meta-fecha">📅 {{ formatearFecha(item.fecha) }}</span>
              <span class="meta-hora" v-if="item.horaInicio && item.horaFin">
                ⏱️ {{ item.horaInicio.slice(0, 5) }} - {{ item.horaFin.slice(0, 5) }}
              </span>
            </div>
            <p v-if="item.observaciones" class="item-obs">"{{ item.observaciones }}"</p>
          </div>
          <div class="item-estado">
            <span class="badge-estado" :class="obtenerClaseEstado(item.estado)">
              {{ item.estado }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { request } from '../services/apiClient'

const props = defineProps({
  visible: { type: Boolean, default: false },
  voluntario: { type: Object, default: null }
})

const emit = defineEmits(['close'])

const historial = ref([])
const cargando = ref(false)
const error = ref('')

const filtroDesde = ref('')
const filtroHasta = ref('')

const cargarHistorial = async () => {
  if (!props.voluntario?.idVoluntario) return
  
  cargando.value = true
  error.value = ''
  try {
    const queryParams = new URLSearchParams()
    if (filtroDesde.value) queryParams.set('desde', filtroDesde.value)
    if (filtroHasta.value) queryParams.set('hasta', filtroHasta.value)
    
    historial.value = await request(
      `/voluntarios/${props.voluntario.idVoluntario}/historial?${queryParams.toString()}`, 
      { mensajeError: 'Error al cargar el historial del voluntario.' }
    )
  } catch (err) {
    error.value = err.message
  } finally {
    cargando.value = false
  }
}

// Recargar cuando el modal se abre o cambian los filtros
watch(
  () => props.visible,
  (newVal) => {
    if (newVal) cargarHistorial()
  },
  { immediate: true }
)

watch([filtroDesde, filtroHasta], () => {
  if (props.visible) cargarHistorial()
})

const limpiarFiltros = () => {
  filtroDesde.value = ''
  filtroHasta.value = ''
}

const cerrarModal = () => {
  emit('close')
}

const formatearFecha = (fechaISO) => {
  if (!fechaISO) return ''
  const partes = fechaISO.split('-')
  return `${partes[2]}/${partes[1]}/${partes[0]}`
}

const obtenerClaseEstado = (estado) => {
  const est = (estado || '').toUpperCase()
  if (est === 'COMPLETADA' || est === 'FINALIZADA' || est === 'ASISTIO') return 'estado-verde'
  if (est === 'CANCELADA' || est === 'NO_ASISTIO') return 'estado-rojo'
  return 'estado-neutro'
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
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
  box-shadow: 0 20px 60px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
}
.modal-ancho {
  width: min(700px, 95%);
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.modal-header h2 {
  margin: 0 0 5px;
  font-family: "DM Serif Display", serif;
  font-size: 26px;
  color: var(--texto);
}
.modal-header p {
  margin: 0;
  font-size: 13px;
  color: var(--texto-suave);
}
.btn-cerrar {
  background: transparent;
  border: none;
  font-size: 28px;
  color: var(--texto-suave);
  cursor: pointer;
  line-height: 1;
}
.btn-cerrar:hover { color: #A34D41; }

.historial-filtros {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  align-items: flex-end;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--crema);
}
.filtro-fecha {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.filtro-fecha label {
  font-size: 11px;
  font-weight: 700;
  color: var(--texto);
}
.filtro-fecha input {
  padding: 10px 14px;
  border: 1px solid var(--crema);
  border-radius: 10px;
  background: var(--crema-claro);
  font-family: inherit;
  font-size: 13px;
  outline: none;
}
.filtro-fecha input:focus { border-color: var(--salvia); }
.btn-limpiar {
  padding: 10px 14px;
  border: 1px dashed var(--salvia-claro);
  background: transparent;
  color: var(--texto-suave);
  border-radius: 10px;
  font-size: 12px;
  cursor: pointer;
}
.btn-limpiar:hover { background: var(--crema-claro); }

.modal-mensaje {
  text-align: center;
  padding: 40px 20px;
  font-size: 14px;
  color: var(--texto-suave);
  flex-grow: 1;
}
.modal-error-msg {
  color: #A34D41;
  background: #FFF0EB;
  border-radius: 12px;
  border: 1px solid #E9B0A5;
}

.historial-lista {
  overflow-y: auto;
  padding-right: 5px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.historial-lista::-webkit-scrollbar { width: 6px; }
.historial-lista::-webkit-scrollbar-thumb { background: var(--crema); border-radius: 10px; }

.historial-item {
  display: flex;
  gap: 15px;
  padding: 18px;
  border: 1px solid var(--crema);
  border-radius: 16px;
  background: #FAFAFA;
  align-items: center;
}
.item-icono {
  width: 45px;
  height: 45px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.icono-tarea { background: #EAF3FA; color: #4B6E91; }
.icono-turno { background: #FDF3E6; color: #D98C4A; }

.item-detalles {
  flex-grow: 1;
}
.item-detalles h4 {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--texto);
}
.item-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--texto-suave);
}
.meta-badge {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.badge-tarea { background: #EAF3FA; color: #4B6E91; border: 1px solid #C9DFEF; }
.badge-turno { background: #FDF3E6; color: #D98C4A; border: 1px solid #F3D9C1; }
.item-obs {
  margin: 8px 0 0;
  font-size: 12px;
  font-style: italic;
  color: var(--texto-suave);
}

.item-estado {
  flex-shrink: 0;
}
.badge-estado {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
}
.estado-verde { background: #E8F2E5; color: #50704B; }
.estado-rojo { background: #FFF0EB; color: #A34D41; }
.estado-neutro { background: #F0F0F0; color: #666; }
</style>