<template>
  <section class="tareas-pagina">
    <div class="tareas-contenedor">
      <div class="tareas-encabezado">
        <span class="tareas-etiqueta">ORGANIZACIÓN DE VOLUNTARIADO</span>
        <h1>Gestión de <em>tareas operativas.</em></h1>
        <p>
          Administra y organiza el trabajo de los voluntarios de Casa Amandita. 
          Consulta las tareas operativas y su avance.
        </p>
      </div>

      <p v-if="accesoDenegado" class="tareas-aviso" role="alert">
        Tu rol no tiene permiso para crear o editar tareas.
      </p>

      <div class="tareas-tarjeta">
        <div class="tareas-tarjeta-encabezado">
          <div>
            <h2>Listado de tareas</h2>
            <p>Consulta el progreso y estado de cada actividad.</p>
          </div>
          <button v-if="puedeGestionarTareas" class="tareas-boton-nuevo" @click="irANuevaTarea">
            Crear Tarea
          </button>
        </div>

        <div v-if="cargando" class="tareas-cargando">
          Cargando tareas desde la base de datos...
        </div>

        <div v-else-if="tareas.length === 0" class="tareas-vacio">
          No hay tareas registradas todavía. Crea la primera con el botón superior.
        </div>

        <div v-else class="tareas-tabla-contenedor">
          <table class="tareas-tabla">
            <thead>
              <tr>
                <th>Título</th>
                <th>Categoría</th>
                <th>Estado</th>
                <th>Avance</th>
                <th v-if="puedeGestionarTareas">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="tarea in tareas" :key="tarea.idTarea">
                <td>
                  <div class="tareas-datos">
                    <strong>{{ tarea.titulo }}</strong>
                    <small>{{ tarea.descripcion }}</small>
                  </div>
                </td>
                <td>
                  <span class="tareas-categoria">{{ tarea.nombreCategoria || 'Sin categoría' }}</span>
                </td>
                <td>
                  <span :class="['tareas-badge', `badge-${(tarea.estado || 'pendiente').toLowerCase().replace(' ', '-')}`]">
                    {{ tarea.estado }}
                  </span>
                </td>
                <td>
                  <strong>{{ tarea.avance ?? 0 }}%</strong>
                </td>
                <td v-if="puedeGestionarTareas">
                  <button 
                    class="tareas-boton-editar" 
                    :disabled="tarea.estado === 'Finalizada' || tarea.estado === 'Cancelada'"
                    @click="editarTarea(tarea.idTarea)"
                    :title="tarea.estado === 'Finalizada' || tarea.estado === 'Cancelada' ? 'No se puede editar una tarea Finalizada o Cancelada' : 'Editar tarea'"
                  >
                    Editar
                  </button>
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
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { tareaService } from '../services/tareaService'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const puedeGestionarTareas = computed(() =>
  ['ADMINISTRADOR', 'GESTOR_TAREAS'].includes(authStore.rol)
)
const accesoDenegado = computed(() => route.query.acceso === 'denegado')
const tareas = ref([])
const cargando = ref(true)

const cargarTareas = async () => {
  cargando.value = true
  try {
    tareas.value = await tareaService.obtenerTareas()
  } catch (err) {
    console.error('Error al consultar tareas:', err)
  } finally {
    cargando.value = false
  }
}

onMounted(() => {
  cargarTareas()
})

const irANuevaTarea = () => {
  router.push('/tareas/nueva')
}

const editarTarea = (id) => {
  router.push(`/tareas/editar/${id}`)
}
</script>

<style scoped>
.tareas-pagina {
  min-height: calc(100vh - 94px);
  padding: 75px 24px 100px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}

.tareas-contenedor {
  width: min(1150px, 100%);
  margin: auto;
}

.tareas-encabezado {
  max-width: 720px;
  margin-bottom: 40px;
}

.tareas-etiqueta {
  font-size: 10px;
  font-weight: 700;
  color: var(--verde-oscuro);
  letter-spacing: 2px;
}

.tareas-encabezado h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-weight: 400;
  font-size: clamp(40px, 5vw, 65px);
  color: var(--texto);
}

.tareas-encabezado h1 em {
  color: var(--verde-oscuro);
}

.tareas-encabezado p {
  max-width: 630px;
  color: var(--texto-suave);
  font-size: 14px;
  line-height: 1.9;
}

.tareas-tarjeta {
  padding: 30px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 23px;
  box-shadow: 0 15px 50px rgba(81, 72, 62, 0.04);
}

.tareas-tarjeta-encabezado {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.tareas-tarjeta-encabezado h2 {
  font-family: "DM Serif Display", serif;
  font-size: 27px;
  font-weight: 400;
  color: var(--texto);
}

.tareas-tarjeta-encabezado p {
  margin-top: 7px;
  font-size: 12px;
  color: var(--texto-suave);
}

.tareas-boton-nuevo {
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

.tareas-boton-nuevo:hover {
  transform: translateY(-2px);
  background: var(--salvia);
}

.tareas-cargando, .tareas-vacio {
  text-align: center;
  padding: 40px;
  color: var(--texto-suave);
  font-size: 14px;
}

.tareas-aviso {
  margin: 0 0 20px;
  padding: 14px 18px;
  border: 1px solid #ef9d87;
  border-radius: 10px;
  background: #fff0eb;
  color: #8d4037;
  font-size: 13px;
}

.tareas-tabla-contenedor {
  overflow-x: auto;
}

.tareas-tabla {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.tareas-tabla th {
  padding: 16px;
  background: var(--crema-claro);
  color: var(--texto-suave);
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.tareas-tabla th:first-child { border-radius: 10px 0 0 10px; }
.tareas-tabla th:last-child { border-radius: 0 10px 10px 0; }

.tareas-tabla td {
  padding: 22px 16px;
  border-bottom: 1px solid var(--crema);
  vertical-align: middle;
}

.tareas-datos {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.tareas-datos strong {
  color: var(--texto);
  font-size: 13px;
}

.tareas-datos small {
  color: var(--texto-suave);
  font-size: 12px;
}

.tareas-categoria {
  font-size: 12px;
  color: var(--texto);
}

.tareas-badge {
  display: inline-flex;
  padding: 8px 13px;
  border-radius: 30px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.badge-pendiente { background: #F8E4D4; color: #A46B4C; }
.badge-en-progreso { background: #E3E9D7; color: #596B43; }
.badge-finalizada { background: #E8F2E5; color: #50704B; }
.badge-cancelada { background: #FFF0EB; color: #A34D41; }

.tareas-boton-editar {
  padding: 8px 16px;
  border: 1px solid var(--salvia-claro);
  border-radius: 10px;
  background: transparent;
  color: var(--texto);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.tareas-boton-editar:hover:not(:disabled) {
  background: var(--crema);
}

.tareas-boton-editar:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 600px) {
  .tareas-pagina { padding: 50px 15px; }
  .tareas-tarjeta { padding: 17px; }
  .tareas-tarjeta-encabezado { flex-direction: column; align-items: flex-start; }
  .tareas-tabla { min-width: 580px; }
}
</style>