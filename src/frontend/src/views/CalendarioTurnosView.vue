<template>
  <section class="calt-pagina">
    <div class="calt-contenedor">
      <span class="calt-etiqueta">CALENDARIO GENERAL DE TURNOS</span>
      <h1>Supervisa la <em>cobertura.</em></h1>
      <p class="calt-descripcion">Consulta los turnos por fecha, actividad y voluntario, y detecta de un vistazo los que tienen cupos sin cubrir.</p>

      <div class="calt-filtros">
        <div class="calt-vistas" role="group" aria-label="Vista del calendario">
          <button
            v-for="v in VISTAS"
            :key="v.valor"
            type="button"
            :class="['calt-vista-boton', { activa: vista === v.valor }]"
            @click="vista = v.valor"
          >
            {{ v.etiqueta }}
          </button>
        </div>

        <label class="calt-filtro">
          <span>Actividad</span>
          <input v-model="filtroActividad" type="text" placeholder="Buscar actividad..." />
        </label>

        <label class="calt-filtro">
          <span>Voluntario</span>
          <select v-model="filtroVoluntario">
            <option value="">Todos</option>
            <option v-for="v in voluntarios" :key="v.idVoluntario" :value="v.idVoluntario">
              {{ v.nombre }} {{ v.apellido }}
            </option>
          </select>
        </label>
      </div>

      <div class="calt-navegacion">
        <button type="button" class="calt-nav-boton" aria-label="Anterior" @click="anterior">←</button>
        <strong class="calt-rango">{{ etiquetaRango }}</strong>
        <button type="button" class="calt-nav-boton" aria-label="Siguiente" @click="siguiente">→</button>
        <button type="button" class="calt-hoy" @click="irAHoy">Hoy</button>
      </div>

      <div class="calt-leyenda">
        <span class="calt-leyenda-item"><i class="calt-punto calt-punto-incompleto"></i> Con cupos sin cubrir</span>
        <span class="calt-leyenda-item"><i class="calt-punto calt-punto-completo"></i> Cobertura completa</span>
      </div>

      <div v-if="error" class="calt-error" role="alert"><span aria-hidden="true">⚠</span> {{ error }}</div>

      <div v-if="cargando" class="calt-mensaje">Cargando calendario...</div>

      <!-- Vista Día: una sola columna con el detalle del día -->
      <div v-else-if="vista === 'dia'" class="calt-dia-vista">
        <p v-if="turnosDelDia(diasVisibles[0]).length === 0" class="calt-sin-turnos">Sin turnos este día.</p>
        <div
          v-for="t in turnosDelDia(diasVisibles[0])"
          :key="t.idTurno"
          :class="['calt-turno-tarjeta', incompleto(t) ? 'incompleto' : 'completo']"
        >
          <strong>{{ t.actividad }}</strong>
          <span>{{ recortar(t.horaInicio) }} – {{ recortar(t.horaFin) }}</span>
          <span class="calt-cupos">{{ t.cuposOcupados }}/{{ t.cuposTotales }} cupos ocupados</span>
        </div>
      </div>

      <!-- Vista Semana: grilla de 7 columnas -->
      <div v-else-if="vista === 'semana'" class="calt-grilla calt-grilla-semana">
        <div v-for="dia in diasVisibles" :key="dia.toISOString()" class="calt-dia">
          <div class="calt-dia-encabezado">{{ etiquetaDia(dia) }}</div>
          <p v-if="turnosDelDia(dia).length === 0" class="calt-sin-turnos">Sin turnos</p>
          <div
            v-for="t in turnosDelDia(dia)"
            :key="t.idTurno"
            :class="['calt-turno-tarjeta', incompleto(t) ? 'incompleto' : 'completo']"
          >
            <strong>{{ t.actividad }}</strong>
            <span>{{ recortar(t.horaInicio) }} – {{ recortar(t.horaFin) }}</span>
            <span class="calt-cupos">{{ t.cuposOcupados }}/{{ t.cuposTotales }}</span>
          </div>
        </div>
      </div>

      <!-- Vista Mes: grilla de semanas completas -->
      <div v-else class="calt-grilla calt-grilla-mes">
        <div
          v-for="dia in diasVisibles"
          :key="dia.toISOString()"
          :class="['calt-dia-mes', { 'fuera-de-mes': dia.getMonth() !== fechaBase.getMonth() }]"
        >
          <div class="calt-dia-mes-numero">{{ dia.getDate() }}</div>
          <div
            v-for="t in turnosDelDia(dia).slice(0, 3)"
            :key="t.idTurno"
            :class="['calt-chip', incompleto(t) ? 'incompleto' : 'completo']"
            :title="`${t.actividad} · ${recortar(t.horaInicio)}-${recortar(t.horaFin)} · ${t.cuposOcupados}/${t.cuposTotales}`"
          >
            {{ t.actividad }}
          </div>
          <div v-if="turnosDelDia(dia).length > 3" class="calt-chip-mas">
            +{{ turnosDelDia(dia).length - 3 }} más
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { turnoService } from '../services/turnoService'
import { voluntarioService } from '../services/voluntarioService'

const VISTAS = [
  { valor: 'dia', etiqueta: 'Día' },
  { valor: 'semana', etiqueta: 'Semana' },
  { valor: 'mes', etiqueta: 'Mes' }
]

const vista = ref('semana')
const fechaBase = ref(normalizar(new Date()))
const filtroActividad = ref('')
const filtroVoluntario = ref('')

const voluntarios = ref([])
const turnos = ref([])
const cargando = ref(true)
const error = ref('')

function normalizar(fecha) {
  const d = new Date(fecha)
  d.setHours(0, 0, 0, 0)
  return d
}

const aISO = (fecha) => {
  const anio = fecha.getFullYear()
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${anio}-${mes}-${dia}`
}

const inicioDeSemana = (fecha) => {
  const d = normalizar(fecha)
  const diaSemana = d.getDay() // 0 = domingo ... 6 = sábado
  const offset = diaSemana === 0 ? -6 : 1 - diaSemana
  d.setDate(d.getDate() + offset)
  return d
}

// Criterio 1: rango de fechas según la vista activa (día, semana o mes)
const rango = computed(() => {
  if (vista.value === 'dia') {
    return { desde: fechaBase.value, hasta: fechaBase.value }
  }
  if (vista.value === 'semana') {
    const inicio = inicioDeSemana(fechaBase.value)
    const fin = new Date(inicio)
    fin.setDate(fin.getDate() + 6)
    return { desde: inicio, hasta: fin }
  }
  // mes: se amplía a semanas completas para que la grilla no quede cortada
  const inicioMes = new Date(fechaBase.value.getFullYear(), fechaBase.value.getMonth(), 1)
  const finMes = new Date(fechaBase.value.getFullYear(), fechaBase.value.getMonth() + 1, 0)
  return { desde: inicioDeSemana(inicioMes), hasta: (() => {
    const fin = inicioDeSemana(finMes)
    fin.setDate(fin.getDate() + 6)
    return fin
  })() }
})

const diasVisibles = computed(() => {
  const { desde, hasta } = rango.value
  const dias = []
  const cursor = new Date(desde)
  while (cursor <= hasta) {
    dias.push(new Date(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }
  return dias
})

const formateadorDia = new Intl.DateTimeFormat('es-ES', { weekday: 'short', day: 'numeric', month: 'short' })
const etiquetaDia = (dia) => formateadorDia.format(dia)

const formateadorCompleto = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'long', year: 'numeric' })
const formateadorCorto = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' })
const formateadorMes = new Intl.DateTimeFormat('es-ES', { month: 'long', year: 'numeric' })

const etiquetaRango = computed(() => {
  if (vista.value === 'dia') return formateadorCompleto.format(fechaBase.value)
  if (vista.value === 'semana') {
    const { desde, hasta } = rango.value
    return `${formateadorCorto.format(desde)} — ${formateadorCorto.format(hasta)}`
  }
  return formateadorMes.format(fechaBase.value)
})

const recortar = (hora) => (hora ? hora.slice(0, 5) : '')

// Criterio 2: un turno con cupos sin cubrir se resalta visualmente
const incompleto = (turno) => (turno.cuposLibres ?? (turno.cuposTotales - turno.cuposOcupados)) > 0

const turnosDelDia = (dia) => {
  const iso = aISO(dia)
  return turnos.value.filter((t) => t.fecha === iso)
}

const anterior = () => {
  const d = new Date(fechaBase.value)
  if (vista.value === 'dia') d.setDate(d.getDate() - 1)
  else if (vista.value === 'semana') d.setDate(d.getDate() - 7)
  else d.setMonth(d.getMonth() - 1)
  fechaBase.value = normalizar(d)
}

const siguiente = () => {
  const d = new Date(fechaBase.value)
  if (vista.value === 'dia') d.setDate(d.getDate() + 1)
  else if (vista.value === 'semana') d.setDate(d.getDate() + 7)
  else d.setMonth(d.getMonth() + 1)
  fechaBase.value = normalizar(d)
}

const irAHoy = () => { fechaBase.value = normalizar(new Date()) }

const cargarVoluntarios = async () => {
  try {
    voluntarios.value = await voluntarioService.obtenerVoluntarios()
  } catch (err) {
    error.value = err.message
  }
}

const cargarCalendario = async () => {
  cargando.value = true
  error.value = ''
  try {
    const { desde, hasta } = rango.value
    const respuesta = await turnoService.obtenerCalendario({
      desde: aISO(desde),
      hasta: aISO(hasta),
      actividad: filtroActividad.value || undefined,
      idVoluntario: filtroVoluntario.value || undefined
    })
    turnos.value = respuesta
  } catch (err) {
    error.value = err.message
    turnos.value = []
  } finally {
    cargando.value = false
  }
}

watch([vista, fechaBase, filtroActividad, filtroVoluntario], cargarCalendario)

onMounted(async () => {
  await cargarVoluntarios()
  await cargarCalendario()
})
</script>

<style scoped>
.calt-pagina {
  min-height: calc(100vh - 94px);
  padding: 60px 24px 90px;
  background: var(--crema-claro);
}

.calt-contenedor {
  width: min(1200px, 100%);
  margin: auto;
  padding: 50px min(5%, 60px);
  box-sizing: border-box;
  background: white;
  border-radius: 30px;
  box-shadow: 0 20px 60px rgba(81, 72, 62, 0.07);
}

.calt-etiqueta { font-size: 10px; letter-spacing: 2.5px; font-weight: 700; color: var(--verde-oscuro); }

.calt-contenedor h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-size: clamp(32px, 3.5vw, 44px);
  font-weight: 400;
  color: var(--texto);
}
.calt-contenedor h1 em { color: var(--verde-oscuro); }

.calt-descripcion { color: var(--texto-suave); font-size: 13px; line-height: 1.8; max-width: 640px; }

.calt-filtros {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px;
  margin-top: 26px;
}

.calt-vistas {
  display: flex;
  border: 1px solid var(--crema);
  border-radius: 30px;
  overflow: hidden;
}

.calt-vista-boton {
  padding: 10px 18px;
  border: none;
  background: var(--crema-claro);
  color: var(--texto-suave);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.calt-vista-boton.activa { background: var(--verde-oscuro); color: white; }

.calt-filtro { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: var(--texto-suave); min-width: 200px; }
.calt-filtro input,
.calt-filtro select {
  padding: 10px 12px;
  background: var(--crema-claro);
  border: 1px solid var(--crema);
  border-radius: 10px;
  color: var(--texto);
  font-family: inherit;
  font-size: 13px;
}

.calt-navegacion { display: flex; align-items: center; justify-content: center; gap: 18px; margin-top: 28px; }
.calt-nav-boton {
  border: 1px solid var(--crema);
  background: var(--crema-claro);
  color: var(--verde-oscuro);
  width: 34px; height: 34px; border-radius: 50%; cursor: pointer; font-size: 14px;
}
.calt-nav-boton:hover { background: var(--crema); }
.calt-rango { color: var(--texto); font-size: 13px; min-width: 220px; text-align: center; text-transform: capitalize; }
.calt-hoy {
  border: 1px solid var(--crema);
  background: white;
  color: var(--texto-suave);
  padding: 7px 14px;
  border-radius: 20px;
  font-size: 11px;
  cursor: pointer;
}

.calt-leyenda { display: flex; gap: 18px; justify-content: center; margin-top: 16px; font-size: 11px; color: var(--texto-suave); }
.calt-leyenda-item { display: inline-flex; align-items: center; gap: 6px; }
.calt-punto { width: 9px; height: 9px; border-radius: 50%; display: inline-block; }
.calt-punto-incompleto { background: #D97757; }
.calt-punto-completo { background: #6FA26A; }

.calt-mensaje { padding: 40px; text-align: center; color: var(--texto-suave); }
.calt-sin-turnos { margin: 0; font-size: 11px; color: var(--texto-suave); }

.calt-error {
  display: flex; align-items: flex-start; gap: 11px; margin-top: 20px; padding: 14px 17px;
  border-radius: 12px; font-size: 12px; line-height: 1.5;
  border: 1px solid #E9B0A5; background: #FFF0EB; color: #A34D41;
}

.calt-dia-vista { margin-top: 28px; display: flex; flex-direction: column; gap: 10px; }

.calt-grilla { display: grid; gap: 10px; margin-top: 28px; }
.calt-grilla-semana { grid-template-columns: repeat(7, minmax(0, 1fr)); }
.calt-grilla-mes { grid-template-columns: repeat(7, minmax(0, 1fr)); }

.calt-dia, .calt-dia-mes {
  padding: 12px; background: var(--crema-claro); border: 1px solid var(--crema); border-radius: 14px; min-height: 90px;
}
.calt-dia { min-height: 150px; }
.calt-dia-mes.fuera-de-mes { opacity: 0.45; }

.calt-dia-encabezado { font-size: 11px; font-weight: 700; color: var(--verde-oscuro); text-transform: capitalize; margin-bottom: 8px; }
.calt-dia-mes-numero { font-size: 12px; font-weight: 700; color: var(--texto); margin-bottom: 6px; }

.calt-turno-tarjeta {
  display: flex; flex-direction: column; gap: 2px; padding: 8px 10px; margin-bottom: 8px;
  background: white; border-radius: 10px; font-size: 11px; color: var(--texto); border-left: 4px solid transparent;
}
.calt-turno-tarjeta.incompleto { border-left-color: #D97757; background: #FFF4EE; }
.calt-turno-tarjeta.completo { border-left-color: #6FA26A; }
.calt-turno-tarjeta span { color: var(--texto-suave); }
.calt-cupos { font-weight: 600; }

.calt-chip {
  padding: 3px 7px; margin-bottom: 4px; border-radius: 8px; font-size: 10px; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis; border-left: 3px solid transparent;
}
.calt-chip.incompleto { background: #FFF4EE; border-left-color: #D97757; color: #8A4A32; }
.calt-chip.completo { background: #EFF6ED; border-left-color: #6FA26A; color: #3F5C3B; }
.calt-chip-mas { font-size: 10px; color: var(--texto-suave); }

@media (max-width: 900px) {
  .calt-grilla-semana { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 600px) {
  .calt-contenedor { padding: 36px 20px; }
  .calt-grilla-semana { grid-template-columns: 1fr; }
  .calt-grilla-mes { grid-template-columns: repeat(7, minmax(0, 1fr)); font-size: 10px; }
}
</style>
