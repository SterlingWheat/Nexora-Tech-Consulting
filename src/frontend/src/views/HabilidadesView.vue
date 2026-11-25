<template>
  <section class="vols-pagina">
    <div class="vols-contenedor">
      <div class="vols-encabezado">
        <span class="vols-etiqueta">CATÁLOGO DE HABILIDADES</span>
        <h1>Gestión de <em>habilidades.</em></h1>
        <p>
          Administra las habilidades técnicas y blandas que pueden ser asignadas a los voluntarios 
          para un mejor emparejamiento con las tareas y turnos.
        </p>
      </div>

      <div class="vols-tarjeta">
        <div class="vols-tarjeta-encabezado">
          <div>
            <h2>Listado de Habilidades</h2>
            <p>Añade, edita o elimina habilidades del catálogo maestro.</p>
          </div>
          <button class="vols-boton-nuevo" @click="abrirModalNuevo">Nueva Habilidad</button>
        </div>

        <div v-if="cargando" class="vols-mensaje">Cargando catálogo...</div>
        <div v-else-if="errorListado" class="vols-mensaje vols-mensaje-error" role="alert">{{ errorListado }}</div>
        
        <div v-else-if="habilidades.length === 0" class="vols-mensaje">
          No hay habilidades registradas en el catálogo.
        </div>

        <div v-else class="vols-tabla-contenedor">
          <table class="vols-tabla">
            <thead>
              <tr>
                <th>Nombre de la Habilidad</th>
                <th>Descripción</th>
                <th style="width: 150px; text-align: center;">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="h in habilidades" :key="h.id">
                <td><strong>{{ h.nombre }}</strong></td>
                <td>{{ h.descripcion || 'Sin descripción' }}</td>
                <td>
                  <div class="acciones-celda" style="justify-content: center;">
                    <button class="vols-boton-editar" @click="abrirModalEditar(h)">Editar</button>
                    <button class="vols-boton-editar btn-desactivar" @click="eliminar(h.id)">Eliminar</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Modal Crear/Editar Habilidad -->
    <div v-if="modalVisible" class="modal-overlay" @click.self="cerrarModal">
      <div class="modal-contenido">
        <h2>{{ esEdicion ? 'Editar Habilidad' : 'Nueva Habilidad' }}</h2>
        <p>Ingresa los detalles de la habilidad.</p>
        
        <form @submit.prevent="guardar" novalidate>
          <div class="modal-campo">
            <label for="nombre">Nombre *</label>
            <input id="nombre" type="text" v-model="formulario.nombre" placeholder="Ej: Primeros Auxilios" maxlength="100" />
          </div>
          
          <div class="modal-campo">
            <label for="descripcion">Descripción</label>
            <input id="descripcion" type="text" v-model="formulario.descripcion" placeholder="Ej: Conocimientos básicos en atención médica" maxlength="255" />
          </div>
          
          <div v-if="errorFormulario" class="modal-error">{{ errorFormulario }}</div>
          
          <div class="modal-acciones">
            <button type="button" class="btn-cancelar" @click="cerrarModal" :disabled="guardando">Cancelar</button>
            <button type="submit" class="btn-guardar" :disabled="guardando || !formulario.nombre.trim()">
              {{ guardando ? 'Guardando...' : 'Guardar' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { habilidadService } from '../services/habilidadService'

const habilidades = ref([])
const cargando = ref(true)
const errorListado = ref('')

const modalVisible = ref(false)
const esEdicion = ref(false)
const guardando = ref(false)
const errorFormulario = ref('')

const formulario = ref({
  id: null,
  nombre: '',
  descripcion: ''
})

const cargarHabilidades = async () => {
  cargando.value = true
  errorListado.value = ''
  try {
    habilidades.value = await habilidadService.obtenerHabilidades()
  } catch (err) {
    errorListado.value = err.message
  } finally {
    cargando.value = false
  }
}

onMounted(cargarHabilidades)

const abrirModalNuevo = () => {
  esEdicion.value = false
  formulario.value = { id: null, nombre: '', descripcion: '' }
  errorFormulario.value = ''
  modalVisible.value = true
}

const abrirModalEditar = (habilidad) => {
  esEdicion.value = true
  formulario.value = { id: habilidad.id, nombre: habilidad.nombre, descripcion: habilidad.descripcion || '' }
  errorFormulario.value = ''
  modalVisible.value = true
}

const cerrarModal = () => {
  if (!guardando.value) modalVisible.value = false
}

const guardar = async () => {
  if (!formulario.value.nombre.trim()) {
    errorFormulario.value = 'El nombre de la habilidad es obligatorio.'
    return
  }
  
  guardando.value = true
  errorFormulario.value = ''
  try {
    const payload = {
      nombre: formulario.value.nombre.trim(),
      descripcion: formulario.value.descripcion.trim() || null
    }

    if (esEdicion.value) {
      await habilidadService.actualizarHabilidad(formulario.value.id, payload)
    } else {
      await habilidadService.crearHabilidad(payload)
    }
    
    modalVisible.value = false
    await cargarHabilidades()
  } catch (err) {
    errorFormulario.value = err.message
  } finally {
    guardando.value = false
  }
}

const eliminar = async (id) => {
  if (!window.confirm('¿Estás seguro de eliminar esta habilidad? Esto la removerá de todos los voluntarios que la posean.')) return
  
  try {
    await habilidadService.eliminarHabilidad(id)
    await cargarHabilidades()
  } catch (err) {
    alert(err.message)
  }
}
</script>

<style scoped>
/* Reutilizando estilos base del sistema */
.vols-pagina {
  min-height: calc(100vh - 94px);
  padding: 75px 24px 100px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}
.vols-contenedor {
  width: min(900px, 100%);
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
.btn-desactivar {
  border-color: #E9B0A5;
  color: #A34D41;
}
.btn-desactivar:hover {
  background: #FFF0EB;
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