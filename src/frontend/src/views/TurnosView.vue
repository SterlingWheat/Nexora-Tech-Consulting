<template>
  <section class="turnos-pagina">
    <div class="turnos-contenedor">
      <div class="turnos-encabezado">
        <span class="turnos-etiqueta">PLANIFICACIÓN DE COBERTURA</span>
        <h1>Gestión de <em>turnos.</em></h1>
        <p>Crea turnos con fecha, horario, actividad y cupos, y consulta cuántos están ocupados.</p>
      </div>

      <div class="turnos-tarjeta">
        <div class="turnos-tarjeta-encabezado">
          <div>
            <h2>Listado de turnos</h2>
            <p>Ordenados por fecha y hora de inicio.</p>
          </div>
          <button class="turnos-boton-nuevo" @click="irANuevo">Crear Turno</button>
        </div>

        <div v-if="cargando" class="turnos-mensaje">Cargando turnos...</div>
        <div v-else-if="error" class="turnos-mensaje turnos-mensaje-error" role="alert">{{ error }}</div>
        <div v-else-if="turnos.length === 0" class="turnos-mensaje">
          No hay turnos creados todavía. Crea el primero con el botón superior.
        </div>

        <div v-else class="turnos-tabla-contenedor">
          <table class="turnos-tabla">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Horario</th>
                <th>Actividad</th>
                <th>Cupos</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in turnos" :key="t.idTurno">
                <td>{{ formatearFecha(t.fecha) }}</td>
                <td>{{ recortar(t.horaInicio) }} – {{ recortar(t.horaFin) }}</td>
                <td><strong>{{ t.actividad }}</strong></td>
                <td>
                  <span :class="['turnos-badge', t.cuposLibres === 0 ? 'badge-lleno' : 'badge-disponible']">
                    {{ t.cuposOcupados }} ocupados / {{ t.cuposLibres }} libres
                  </span>
                  <small class="turnos-total">de {{ t.cuposTotales }}</small>
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
import { turnoService } from '../services/turnoService'

const router = useRouter()
const turnos = ref([])
const cargando = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    turnos.value = await turnoService.obtenerTurnos()
  } catch (err) {
    error.value = err.message
  } finally {
    cargando.value = false
  }
})

// El backend devuelve HH:mm:ss
const recortar = (hora) => (hora ? hora.slice(0, 5) : '')

// Se arma la fecha en local para no desfasar un día por la zona horaria
const formatearFecha = (fecha) => {
  if (!fecha) return '—'
  const [a, m, d] = fecha.split('-').map(Number)
  return new Date(a, m - 1, d).toLocaleDateString('es-BO', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' })
}

const irANuevo = () => router.push('/turnos/nuevo')
</script>

<style scoped>
.turnos-pagina {
  min-height: calc(100vh - 94px);
  padding: 75px 24px 100px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}

.turnos-contenedor { width: min(1150px, 100%); margin: auto; }
.turnos-encabezado { max-width: 720px; margin-bottom: 40px; }

.turnos-etiqueta {
  font-size: 10px;
  font-weight: 700;
  color: var(--verde-oscuro);
  letter-spacing: 2px;
}

.turnos-encabezado h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-weight: 400;
  font-size: clamp(40px, 5vw, 65px);
  color: var(--texto);
}

.turnos-encabezado h1 em { color: var(--verde-oscuro); }

.turnos-encabezado p {
  max-width: 630px;
  color: var(--texto-suave);
  font-size: 14px;
  line-height: 1.9;
}

.turnos-tarjeta {
  padding: 30px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 23px;
  box-shadow: 0 15px 50px rgba(81, 72, 62, 0.04);
}

.turnos-tarjeta-encabezado {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.turnos-tarjeta-encabezado h2 {
  font-family: "DM Serif Display", serif;
  font-size: 27px;
  font-weight: 400;
  color: var(--texto);
}

.turnos-tarjeta-encabezado p { margin-top: 7px; font-size: 12px; color: var(--texto-suave); }

.turnos-boton-nuevo {
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

.turnos-boton-nuevo:hover { transform: translateY(-2px); background: var(--salvia); }

.turnos-mensaje { text-align: center; padding: 40px; color: var(--texto-suave); font-size: 14px; }
.turnos-mensaje-error { color: #A34D41; }

.turnos-tabla-contenedor { overflow-x: auto; }
.turnos-tabla { width: 100%; border-collapse: collapse; text-align: left; }

.turnos-tabla th {
  padding: 16px;
  background: var(--crema-claro);
  color: var(--texto-suave);
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.turnos-tabla th:first-child { border-radius: 10px 0 0 10px; }
.turnos-tabla th:last-child { border-radius: 0 10px 10px 0; }

.turnos-tabla td {
  padding: 22px 16px;
  border-bottom: 1px solid var(--crema);
  vertical-align: middle;
  font-size: 13px;
  color: var(--texto);
}

.turnos-badge {
  display: inline-flex;
  padding: 8px 13px;
  border-radius: 30px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.badge-disponible { background: #E8F2E5; color: #50704B; }
.badge-lleno { background: #FFF0EB; color: #A34D41; }
.turnos-total { margin-left: 8px; color: var(--texto-suave); }

@media (max-width: 600px) {
  .turnos-pagina { padding: 50px 15px; }
  .turnos-tarjeta { padding: 17px; }
  .turnos-tarjeta-encabezado { flex-direction: column; align-items: flex-start; }
  .turnos-tabla { min-width: 620px; }
}
</style>
