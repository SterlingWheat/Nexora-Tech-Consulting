<template>
  <section class="vols-pagina">
    <div class="vols-contenedor">
      <div class="vols-encabezado">
        <span class="vols-etiqueta">PADRÓN DE VOLUNTARIOS</span>
        <h1>Gestión de <em>voluntarios.</em></h1>
        <p>
          Registra voluntarios y mantén sus datos personales al día. 
          Habilita su acceso si necesitan ingresar al sistema.
        </p>
      </div>

      <div class="vols-tarjeta">
        <div class="vols-tarjeta-encabezado">
          <div>
            <h2>Listado de voluntarios</h2>
            <p>Consulta, edita o habilita cuentas de acceso.</p>
          </div>
          <button class="vols-boton-nuevo" @click="irANuevo">Registrar Voluntario</button>
        </div>

        <div class="vols-filtros" role="search" aria-label="Buscar y filtrar voluntarios">
          <input
            v-model="texto"
            type="search"
            class="vols-buscador"
            placeholder="Buscar por nombre, apellido o documento..."
            aria-label="Buscar voluntarios"
          />

          <div class="vols-filtro-grupo" role="group" aria-label="Filtrar por estado">
            <button
              v-for="opcion in filtrosEstado"
              :key="opcion.valor"
              type="button"
              class="vols-filtro-boton"
              :class="{ activo: filtroEstado === opcion.valor }"
              @click="filtroEstado = opcion.valor"
            >
              {{ opcion.etiqueta }}
            </button>
          </div>

          <select v-model="diaSemana" class="vols-filtro-dia" aria-label="Filtrar por día de disponibilidad">
            <option value="">Cualquier disponibilidad</option>
            <option v-for="dia in DIAS" :key="dia.valor" :value="dia.valor">Disponible el {{ dia.etiqueta }}</option>
          </select>
        </div>

        <div v-if="cargando" class="vols-mensaje">Cargando voluntarios...</div>
        <div v-else-if="error" class="vols-mensaje vols-mensaje-error" role="alert">{{ error }}</div>

        <div v-else-if="voluntarios.length === 0" class="vols-mensaje">
          {{ hayFiltrosActivos
            ? 'Ningún voluntario coincide con la búsqueda o los filtros aplicados.'
            : 'No hay voluntarios registrados todavía. Registra el primero con el botón superior.' }}
        </div>

        <div v-else class="vols-tabla-contenedor">
          <table class="vols-tabla">
            <thead>
              <tr>
                <th>Voluntario</th>
                <th>Documento</th>
                <th>Contacto</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="v in voluntarios" :key="v.idVoluntario">
                <td>
                  <strong>{{ v.nombre }} {{ v.apellido }}</strong>
                  <span v-if="v.idUsuario" class="badge-acceso">Tiene Cuenta</span>
                </td>
                <td>{{ v.documento }}</td>
                <td>
                  <div class="vols-datos">
                    <span>{{ v.email }}</span>
                    <small>{{ v.telefono || 'Sin teléfono' }}</small>
                  </div>
                </td>
                <td>
                  <span :class="['vols-badge', v.activo ? 'badge-activo' : 'badge-inactivo']">
                    {{ v.activo ? 'Activo' : 'Inactivo' }}
                  </span>
                </td>
                <td>
                  <div class="acciones-celda">
                    <button class="vols-boton-editar" @click="editar(v.idVoluntario)">Editar</button>
                    <button class="vols-boton-editar vols-boton-disp" @click="verDisponibilidad(v.idVoluntario)">Disponibilidad</button>
                    
                    <!-- Botón lógico de Habilitar Acceso si idUsuario es null -->
                    <button 
                      v-if="!v.idUsuario" 
                      class="vols-boton-editar btn-habilitar" 
                      @click="abrirModalAcceso(v)"
                    >
                      Habilitar acceso
                    </button>
                    
                    <!-- Solución Error 2: Botones para desactivar y activar directamente desde aquí -->
                    <button 
                      v-if="v.activo" 
                      class="vols-boton-editar btn-desactivar" 
                      @click="desactivar(v.idVoluntario)"
                    >
                      Desactivar
                    </button>
                    <button
                      v-else
                      class="vols-boton-editar btn-activar"
                      @click="abrirModalActivar(v)"
                    >
                      Activar
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <Paginacion
            :paginaActual="paginaActual"
            :totalPaginas="totalPaginas"
            @cambiarPagina="cargarVoluntarios"
          />
          <p class="vols-total" v-if="totalElementos > 0">{{ totalElementos }} voluntario(s) en total.</p>
        </div>
      </div>
    </div>

    <!-- Modal Habilitar Acceso -->
    <div v-if="modalVisible" class="modal-overlay" @click.self="cerrarModal">
      <div class="modal-contenido">
        <h2>Habilitar Acceso</h2>
        <p>Se creará una cuenta con rol VOLUNTARIO para <strong>{{ voluntarioSeleccionado.nombre }}</strong> usando su correo registrado (<strong>{{ voluntarioSeleccionado.email }}</strong>).</p>
        
        <div class="modal-campo">
          <label>Contraseña temporal (Mín. 8 caracteres)</label>
          <input type="password" v-model="passwordTemporal" placeholder="Escribe una contraseña segura" />
        </div>
        
        <div v-if="errorModal" class="modal-error">{{ errorModal }}</div>
        
        <div class="modal-acciones">
          <button class="btn-cancelar" @click="cerrarModal">Cancelar</button>
          <button class="btn-guardar" @click="confirmarAcceso" :disabled="guardandoAcceso || passwordTemporal.length < 8">
            {{ guardandoAcceso ? 'Creando...' : 'Crear Cuenta' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Activar Voluntario -->
    <div v-if="modalActivarVisible" class="modal-overlay" @click.self="cerrarModalActivar">
      <div class="modal-contenido">
        <h2>Activar voluntario</h2>
        <p>
          ¿Estás seguro de que deseas activar a
          <strong>{{ voluntarioParaActivar?.nombre }} {{ voluntarioParaActivar?.apellido }}</strong>?
          Volverá a estar disponible para asignaciones a tareas y turnos.
        </p>

        <div v-if="errorActivar" class="modal-error">{{ errorActivar }}</div>

        <div class="modal-acciones">
          <button class="btn-cancelar" @click="cerrarModalActivar" :disabled="activandoConfirmacion">Cancelar</button>
          <button class="btn-guardar" @click="confirmarActivar" :disabled="activandoConfirmacion">
            {{ activandoConfirmacion ? 'Activando...' : 'Activar' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { voluntarioService } from '../services/voluntarioService'
import { DIAS } from '../services/disponibilidadService'
import Paginacion from '../components/Paginacion.vue'

const router = useRouter()
const voluntarios = ref([])
const cargando = ref(true)
const error = ref('')

const modalVisible = ref(false)
const voluntarioSeleccionado = ref(null)
const passwordTemporal = ref('')
const errorModal = ref('')
const guardandoAcceso = ref(false)

const modalActivarVisible = ref(false)
const voluntarioParaActivar = ref(null)
const errorActivar = ref('')
const activandoConfirmacion = ref(false)

// KAN-27: búsqueda por texto, filtros de estado/disponibilidad y paginación
const PAGE_SIZE = 10
const filtrosEstado = [
  { valor: 'todos', etiqueta: 'Todos' },
  { valor: 'activos', etiqueta: 'Activos' },
  { valor: 'inactivos', etiqueta: 'Inactivos' },
]
const texto = ref('')
const filtroEstado = ref('todos')
const diaSemana = ref('')
const paginaActual = ref(0)
const totalPaginas = ref(0)
const totalElementos = ref(0)

const hayFiltrosActivos = computed(() =>
  Boolean(texto.value.trim()) || filtroEstado.value !== 'todos' || Boolean(diaSemana.value)
)

const activoParaFiltro = () => {
  if (filtroEstado.value === 'activos') return true
  if (filtroEstado.value === 'inactivos') return false
  return null
}

const cargarVoluntarios = async (page = 0) => {
  cargando.value = true
  error.value = ''
  try {
    const respuesta = await voluntarioService.buscarVoluntarios({
      texto: texto.value.trim() || undefined,
      activo: activoParaFiltro(),
      diaSemana: diaSemana.value || undefined,
      page,
      size: PAGE_SIZE,
    })
    voluntarios.value = respuesta.content
    paginaActual.value = respuesta.number
    totalPaginas.value = respuesta.totalPages
    totalElementos.value = respuesta.totalElements
  } catch (err) {
    error.value = err.message
  } finally {
    cargando.value = false
  }
}

// Debounce de la búsqueda por texto para no disparar una petición por cada tecla
let debounceId = null
watch(texto, () => {
  clearTimeout(debounceId)
  debounceId = setTimeout(() => cargarVoluntarios(0), 350)
})

watch([filtroEstado, diaSemana], () => cargarVoluntarios(0))

onMounted(() => cargarVoluntarios(0))

const irANuevo = () => router.push('/voluntarios/nuevo')
const editar = (id) => router.push(`/voluntarios/editar/${id}`)
const verDisponibilidad = (id) => router.push(`/voluntarios/${id}/disponibilidad`)

const abrirModalAcceso = (voluntario) => {
  voluntarioSeleccionado.value = voluntario
  passwordTemporal.value = ''
  errorModal.value = ''
  modalVisible.value = true
}

const cerrarModal = () => {
  if (!guardandoAcceso.value) {
    modalVisible.value = false
  }
}

const confirmarAcceso = async () => {
  if (passwordTemporal.value.trim().length < 8) {
    errorModal.value = 'La contraseña debe tener al menos 8 caracteres.'
    return
  }
  
  guardandoAcceso.value = true
  errorModal.value = ''
  try {
    await voluntarioService.habilitarAcceso(voluntarioSeleccionado.value.idVoluntario, passwordTemporal.value)
    modalVisible.value = false // Modificamos la variable directamente para forzar el cierre
    await cargarVoluntarios(paginaActual.value) // Refresca la página actual
  } catch (err) {
    errorModal.value = err.message
  } finally {
    guardandoAcceso.value = false
  }
}

// Solución Error 2: Funciones para Activar y Desactivar voluntarios
const abrirModalActivar = (voluntario) => {
  voluntarioParaActivar.value = voluntario
  errorActivar.value = ''
  modalActivarVisible.value = true
}

const cerrarModalActivar = () => {
  if (!activandoConfirmacion.value) {
    modalActivarVisible.value = false
  }
}

const confirmarActivar = async () => {
  activandoConfirmacion.value = true
  errorActivar.value = ''
  try {
    await voluntarioService.activarVoluntario(voluntarioParaActivar.value.idVoluntario)
    modalActivarVisible.value = false
    await cargarVoluntarios(paginaActual.value)
  } catch (err) {
    errorActivar.value = err.message
  } finally {
    activandoConfirmacion.value = false
  }
}

const desactivar = async (idVoluntario) => {
  if (!window.confirm('¿Estás seguro de que deseas desactivar a este voluntario? (Esto también desactivará su acceso al sistema)')) return
  error.value = ''
  cargando.value = true
  try {
    await voluntarioService.desactivarVoluntario(idVoluntario)
    await cargarVoluntarios(paginaActual.value)
  } catch (err) {
    error.value = err.message
    cargando.value = false
  }
}
</script>

<style scoped>
.vols-pagina {
  min-height: calc(100vh - 94px);
  padding: 75px 24px 100px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}
.vols-contenedor {
  width: min(1150px, 100%);
  margin: auto;
}
.vols-encabezado {
  max-width: 720px;
  margin-bottom: 40px;
}
.vols-etiqueta {
  font-size: 10px;
  font-weight: 700;
  color: var(--verde-oscuro);
  letter-spacing: 2px;
}
.vols-encabezado h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-weight: 400;
  font-size: clamp(40px, 5vw, 65px);
  color: var(--texto);
}
.vols-encabezado h1 em {
  color: var(--verde-oscuro);
}
.vols-encabezado p {
  max-width: 630px;
  color: var(--texto-suave);
  font-size: 14px;
  line-height: 1.9;
}
.vols-tarjeta {
  padding: 30px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 23px;
  box-shadow: 0 15px 50px rgba(81, 72, 62, 0.04);
}
.vols-tarjeta-encabezado {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}
.vols-tarjeta-encabezado h2 {
  font-family: "DM Serif Display", serif;
  font-size: 27px;
  font-weight: 400;
  color: var(--texto);
  margin: 0;
}
.vols-tarjeta-encabezado p {
  margin-top: 7px;
  margin-bottom: 0;
  font-size: 12px;
  color: var(--texto-suave);
}
.vols-boton-nuevo {
  padding: 12px 24px;
  border-radius: 30px;
  background: var(--verde-oscuro);
  color: white;
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: transform 0.2s, background 0.2s;
}
.vols-boton-nuevo:hover {
  transform: translateY(-2px);
  background: var(--salvia);
}

.vols-filtros {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}
.vols-buscador {
  flex: 1 1 220px;
  padding: 10px 16px;
  border: 1px solid var(--crema);
  border-radius: 30px;
  background: var(--crema-claro);
  font-family: inherit;
  font-size: 13px;
  outline: none;
}
.vols-buscador:focus {
  border-color: var(--salvia);
}
.vols-filtro-grupo {
  display: flex;
  gap: 8px;
}
.vols-filtro-boton {
  padding: 8px 18px;
  border-radius: 30px;
  border: 1px solid var(--salvia-claro);
  background: transparent;
  color: var(--texto-suave);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.vols-filtro-boton:hover {
  background: var(--crema);
}
.vols-filtro-boton.activo {
  background: var(--verde-oscuro);
  color: white;
  border-color: var(--verde-oscuro);
}
.vols-filtro-dia {
  padding: 9px 14px;
  border: 1px solid var(--crema);
  border-radius: 30px;
  background: var(--crema-claro);
  font-family: inherit;
  font-size: 12px;
  color: var(--texto);
  outline: none;
}
.vols-total {
  text-align: center;
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--texto-suave);
}

.vols-mensaje {
  text-align: center;
  padding: 40px;
  color: var(--texto-suave);
  font-size: 14px;
}
.vols-mensaje-error {
  color: #A34D41;
}

.vols-tabla-contenedor {
  overflow-x: auto;
}
.vols-tabla {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
.vols-tabla th {
  padding: 16px;
  background: var(--crema-claro);
  color: var(--texto-suave);
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}
.vols-tabla th:first-child { border-radius: 10px 0 0 10px; }
.vols-tabla th:last-child { border-radius: 0 10px 10px 0; }
.vols-tabla td {
  padding: 22px 16px;
  border-bottom: 1px solid var(--crema);
  vertical-align: middle;
  font-size: 13px;
  color: var(--texto);
}

.vols-datos {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.vols-datos small {
  color: var(--texto-suave);
  font-size: 12px;
}

.badge-acceso {
  display: block;
  font-size: 10px;
  color: #4B6E91;
  background: #D9E5F2;
  padding: 2px 8px;
  border-radius: 10px;
  width: fit-content;
  margin-top: 4px;
  font-weight: bold;
}
.vols-badge {
  display: inline-flex;
  padding: 8px 13px;
  border-radius: 30px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}
.badge-activo { background: #E8F2E5; color: #50704B; }
.badge-inactivo { background: #FFF0EB; color: #A34D41; }

.acciones-celda {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.vols-boton-editar {
  padding: 8px 16px;
  border: 1px solid var(--salvia-claro);
  border-radius: 10px;
  background: transparent;
  color: var(--texto);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.vols-boton-editar:hover {
  background: var(--crema);
}
.btn-habilitar {
  border-color: #4B6E91;
  color: #4B6E91;
}
.btn-habilitar:hover {
  background: #D9E5F2;
}
.btn-desactivar {
  border-color: #E9B0A5;
  color: #A34D41;
}
.btn-desactivar:hover {
  background: #FFF0EB;
}
.btn-activar {
  border-color: var(--verde-oscuro);
  color: var(--verde-oscuro);
}
.btn-activar:hover {
  background: #f1f5ec;
}

/* MODAL */
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
}
.modal-contenido h2 {
  margin: 0 0 10px;
  font-family: "DM Serif Display", serif;
  font-size: 24px;
}
.modal-contenido p {
  font-size: 13px;
  color: var(--texto-suave);
  line-height: 1.5;
  margin-bottom: 20px;
}
.modal-campo {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}
.modal-campo label {
  font-size: 12px;
  font-weight: bold;
}
.modal-campo input {
  padding: 12px 16px;
  border: 1px solid var(--crema);
  border-radius: 12px;
  background: var(--crema-claro);
  outline: none;
  font-family: inherit;
  font-size: 13px;
}
.modal-campo input:focus {
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
.btn-guardar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>