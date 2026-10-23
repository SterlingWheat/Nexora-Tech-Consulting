<template>
  <section class="vols-pagina">
    <div class="vols-contenedor">
      <div class="vols-encabezado">
        <span class="vols-etiqueta">PADRÓN DE VOLUNTARIOS</span>
        <h1>Gestión de <em>voluntarios.</em></h1>
        <p>
          Registra voluntarios y mantén sus datos personales y de contacto al día.
        </p>
      </div>

      <div class="vols-tarjeta">
        <div class="vols-tarjeta-encabezado">
          <div>
            <h2>Listado de voluntarios</h2>
            <p>Consulta y actualiza los datos de cada voluntario.</p>
          </div>
          <button class="vols-boton-nuevo" @click="irANuevo">Registrar Voluntario</button>
        </div>

        <div v-if="cargando" class="vols-mensaje">Cargando voluntarios...</div>

        <div v-else-if="error" class="vols-mensaje vols-mensaje-error" role="alert">{{ error }}</div>

        <div v-else-if="voluntarios.length === 0" class="vols-mensaje">
          No hay voluntarios registrados todavía. Registra el primero con el botón superior.
        </div>

        <div v-else class="vols-tabla-contenedor">
          <table class="vols-tabla">
            <thead>
              <tr>
                <th>Voluntario</th>
                <th>Documento</th>
                <th>Contacto</th>
                <th>Estado</th>
                <th>Última modificación</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="v in voluntarios" :key="v.idVoluntario">
                <td><strong>{{ v.nombre }} {{ v.apellido }}</strong></td>
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
                <td>{{ formatearFecha(v.fechaModificacion) }}</td>
                <td>
                  <button class="vols-boton-editar" @click="editar(v.idVoluntario)">Editar</button>
                  <button class="vols-boton-editar vols-boton-disp" @click="verDisponibilidad(v.idVoluntario)">Disponibilidad</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { voluntarioService } from '../services/voluntarioService'

const router = useRouter()
const voluntarios = ref([])
const cargando = ref(true)
const error = ref('')

const cargarVoluntarios = async () => {
  cargando.value = true
  error.value = ''
  try {
    voluntarios.value = await voluntarioService.obtenerVoluntarios()
  } catch (err) {
    error.value = err.message
  } finally {
    cargando.value = false
  }
}

onMounted(cargarVoluntarios)

const formatearFecha = (fecha) => {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleString('es-BO', { dateStyle: 'short', timeStyle: 'short' })
}

const irANuevo = () => router.push('/voluntarios/nuevo')
const editar = (id) => router.push(`/voluntarios/editar/${id}`)
const verDisponibilidad = (id) => router.push(`/voluntarios/${id}/disponibilidad`)
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

.vols-datos {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.vols-datos small {
  color: var(--texto-suave);
  font-size: 12px;
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

.vols-boton-disp {
  margin-left: 8px;
}

.vols-boton-editar:hover {
  background: var(--crema);
}

@media (max-width: 600px) {
  .vols-pagina { padding: 50px 15px; }
  .vols-tarjeta { padding: 17px; }
  .vols-tarjeta-encabezado { flex-direction: column; align-items: flex-start; }
  .vols-tabla { min-width: 700px; }
}
</style>
