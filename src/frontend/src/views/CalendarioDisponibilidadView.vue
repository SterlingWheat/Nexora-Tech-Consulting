<template>
  <section class="cal-pagina">
    <div class="cal-contenedor">
      <span class="cal-etiqueta">CALENDARIO DE DISPONIBILIDAD</span>
      <h1>Planifica <em>turnos visualmente.</em></h1>
      <p class="cal-descripcion">Consulta las franjas disponibles de los voluntarios semana a semana.</p>

      <div class="cal-filtros">
        <label class="cal-filtro">
          <span>Voluntario</span>
          <select v-model="idVoluntario">
            <option value="">Todos</option>
            <option v-for="v in voluntarios" :key="v.idVoluntario" :value="v.idVoluntario">
              {{ v.nombre }} {{ v.apellido }}
            </option>
          </select>
        </label>

        <label class="cal-filtro">
          <span>Habilidad</span>
          <select v-model="idHabilidad">
            <option value="">Todas</option>
            <option v-for="h in habilidades" :key="h.id" :value="h.id">
              {{ h.nombre }}
            </option>
          </select>
        </label>
      </div>

      <div class="cal-navegacion">
        <button type="button" class="cal-nav-boton" aria-label="Semana anterior" @click="semanaAnterior">←</button>
        <strong class="cal-semana-rango">{{ rangoSemana }}</strong>
        <button type="button" class="cal-nav-boton" aria-label="Semana siguiente" @click="semanaSiguiente">→</button>
      </div>

      <div v-if="error" class="cal-error" role="alert"><span aria-hidden="true">⚠</span> {{ error }}</div>

      <div v-if="cargando" class="cal-mensaje">Cargando calendario...</div>

      <div v-else class="cal-grilla">
        <div v-for="dia in diasSemana" :key="dia.toISOString()" class="cal-dia">
          <div class="cal-dia-encabezado">{{ etiquetaDia(dia) }}</div>

          <p v-if="franjasDelDia(dia).length === 0" class="cal-sin-franjas">Sin disponibilidad</p>

          <div v-for="f in franjasDelDia(dia)" :key="`${f.idVoluntario}-${f.horaInicio}-${f.fecha}`" class="cal-franja">
            <strong>{{ f.nombreVoluntario }} {{ f.apellidoVoluntario }}</strong>
            <span>{{ recortar(f.horaInicio) }} – {{ recortar(f.horaFin) }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { calendarioDisponibilidadService } from '../services/calendarioDisponibilidadService'
import { voluntarioService } from '../services/voluntarioService'
import { habilidadService } from '../services/habilidadService'

const voluntarios = ref([])
const habilidades = ref([])
const idVoluntario = ref('')
const idHabilidad = ref('')
const franjas = ref([])
const cargando = ref(true)
const error = ref('')

// El backend devuelve HH:mm:ss; se muestra solo HH:mm
const recortar = (hora) => (hora ? hora.slice(0, 5) : '')

const aISO = (fecha) => {
  const anio = fecha.getFullYear()
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${anio}-${mes}-${dia}`
}

const inicioDeSemana = (fecha) => {
  const d = new Date(fecha)
  const diaSemana = d.getDay() // 0 = domingo ... 6 = sábado
  const offset = diaSemana === 0 ? -6 : 1 - diaSemana
  d.setDate(d.getDate() + offset)
  d.setHours(0, 0, 0, 0)
  return d
}

const inicioSemana = ref(inicioDeSemana(new Date()))

const diasSemana = computed(() => {
  const dias = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(inicioSemana.value)
    d.setDate(d.getDate() + i)
    dias.push(d)
  }
  return dias
})

const formateador = new Intl.DateTimeFormat('es-ES', { weekday: 'short', day: 'numeric', month: 'short' })
const etiquetaDia = (dia) => formateador.format(dia)

const formateadorRango = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short', year: 'numeric' })
const rangoSemana = computed(() => {
  const dias = diasSemana.value
  return `${formateadorRango.format(dias[0])} — ${formateadorRango.format(dias[6])}`
})

const franjasDelDia = (dia) => {
  const iso = aISO(dia)
  return franjas.value
    .filter((f) => f.fecha === iso)
    .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio) || a.nombreVoluntario.localeCompare(b.nombreVoluntario))
}

const semanaAnterior = () => {
  const d = new Date(inicioSemana.value)
  d.setDate(d.getDate() - 7)
  inicioSemana.value = d
}

const semanaSiguiente = () => {
  const d = new Date(inicioSemana.value)
  d.setDate(d.getDate() + 7)
  inicioSemana.value = d
}

const cargarFiltros = async () => {
  try {
    voluntarios.value = await voluntarioService.obtenerVoluntarios()
  } catch (err) {
    error.value = err.message
  }

  // El catálogo de habilidades solo lo puede listar Administrador/Gestor de Tareas;
  // si TI no tiene acceso, el filtro de habilidad simplemente queda vacío (no rompe la página)
  try {
    habilidades.value = await habilidadService.obtenerHabilidades()
  } catch {
    habilidades.value = []
  }
}

// Criterio 1: al seleccionar una semana se ven las franjas disponibles por voluntario
// Criterio 2: el calendario se actualiza al aplicar un filtro de voluntario o habilidad
const cargarCalendario = async () => {
  cargando.value = true
  error.value = ''
  try {
    const dias = diasSemana.value
    const respuesta = await calendarioDisponibilidadService.obtenerCalendario({
      desde: aISO(dias[0]),
      hasta: aISO(dias[6]),
      idVoluntario: idVoluntario.value || undefined,
      idHabilidad: idHabilidad.value || undefined
    })
    franjas.value = respuesta.franjas
  } catch (err) {
    error.value = err.message
    franjas.value = []
  } finally {
    cargando.value = false
  }
}

watch([inicioSemana, idVoluntario, idHabilidad], cargarCalendario)

onMounted(async () => {
  await cargarFiltros()
  await cargarCalendario()
})
</script>

<style scoped>
.cal-pagina {
  min-height: calc(100vh - 94px);
  padding: 60px 24px 90px;
  background: var(--crema-claro);
}

.cal-contenedor {
  width: min(1100px, 100%);
  margin: auto;
  padding: 50px min(5%, 60px);
  box-sizing: border-box;
  background: white;
  border-radius: 30px;
  box-shadow: 0 20px 60px rgba(81, 72, 62, 0.07);
}

.cal-etiqueta {
  font-size: 10px;
  letter-spacing: 2.5px;
  font-weight: 700;
  color: var(--verde-oscuro);
}

.cal-contenedor h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-size: clamp(32px, 3.5vw, 44px);
  font-weight: 400;
  color: var(--texto);
}

.cal-contenedor h1 em { color: var(--verde-oscuro); }

.cal-descripcion {
  color: var(--texto-suave);
  font-size: 13px;
  line-height: 1.8;
}

.cal-filtros {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 26px;
}

.cal-filtro {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--texto-suave);
  min-width: 200px;
}

.cal-filtro select {
  padding: 10px 12px;
  background: var(--crema-claro);
  border: 1px solid var(--crema);
  border-radius: 10px;
  color: var(--texto);
  font-family: inherit;
  font-size: 13px;
}

.cal-navegacion {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin-top: 28px;
}

.cal-nav-boton {
  border: 1px solid var(--crema);
  background: var(--crema-claro);
  color: var(--verde-oscuro);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
}

.cal-nav-boton:hover { background: var(--crema); }

.cal-semana-rango {
  color: var(--texto);
  font-size: 13px;
  min-width: 220px;
  text-align: center;
}

.cal-mensaje { padding: 40px; text-align: center; color: var(--texto-suave); }

.cal-error {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  margin-top: 20px;
  padding: 14px 17px;
  border-radius: 12px;
  font-size: 12px;
  line-height: 1.5;
  border: 1px solid #E9B0A5;
  background: #FFF0EB;
  color: #A34D41;
}

.cal-grilla {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  margin-top: 28px;
}

.cal-dia {
  padding: 14px;
  background: var(--crema-claro);
  border: 1px solid var(--crema);
  border-radius: 14px;
  min-height: 140px;
}

.cal-dia-encabezado {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--verde-oscuro);
  text-transform: capitalize;
  margin-bottom: 10px;
}

.cal-sin-franjas {
  margin: 0;
  font-size: 11px;
  color: var(--texto-suave);
}

.cal-franja {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  margin-bottom: 8px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 10px;
  font-size: 11px;
  color: var(--texto);
}

.cal-franja span { color: var(--texto-suave); }

@media (max-width: 900px) {
  .cal-grilla { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 600px) {
  .cal-contenedor { padding: 36px 20px; }
  .cal-grilla { grid-template-columns: 1fr; }
}
</style>
