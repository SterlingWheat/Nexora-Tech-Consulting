<template>
  <section class="turno-form">
    <div class="turno-contenedor">
      <span class="turno-etiqueta">NUEVO TURNO</span>
      <h1>Crear <em>turno.</em></h1>
      <p class="turno-descripcion">
        Define la fecha, el horario, la actividad y los cupos para planificar la cobertura.
      </p>

      <form class="turno-formulario" novalidate @submit.prevent="guardarTurno">
        <div class="turno-campo">
          <label for="fecha">Fecha *</label>
          <input id="fecha" type="date" v-model="formulario.fecha" :min="hoy" />
        </div>

        <div class="turno-fila">
          <div class="turno-campo">
            <label for="horaInicio">Hora de inicio *</label>
            <input id="horaInicio" type="time" v-model="formulario.horaInicio" />
          </div>
          <div class="turno-campo">
            <label for="horaFin">Hora de fin *</label>
            <input id="horaFin" type="time" v-model="formulario.horaFin" />
          </div>
        </div>

        <div class="turno-campo">
          <label for="actividad">Actividad *</label>
          <input id="actividad" type="text" v-model="formulario.actividad" placeholder="Ej: Clasificar alimentos" maxlength="200" />
        </div>

        <div class="turno-campo">
          <label for="cuposTotales">Cupos *</label>
          <input id="cuposTotales" type="number" min="1" max="500" v-model.number="formulario.cuposTotales" />
        </div>

        <div v-if="error" class="turno-error" role="alert">
          <span aria-hidden="true">⚠</span> {{ error }}
        </div>

        <button type="submit" class="turno-boton" :disabled="cargando">
          {{ cargando ? 'Guardando...' : 'Crear Turno' }}
        </button>
      </form>

      <button type="button" class="turno-volver" @click="volver">← Volver a turnos</button>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { turnoService } from '../services/turnoService'

const router = useRouter()

const cargando = ref(false)
const error = ref('')

// Fecha local en formato YYYY-MM-DD (toISOString usaría UTC y podría adelantar el día)
const aFechaLocal = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const hoy = aFechaLocal(new Date())

const formulario = ref({
  fecha: '',
  horaInicio: '',
  horaFin: '',
  actividad: '',
  cuposTotales: 1
})

// Criterio 2: validaciones del lado del cliente (el backend vuelve a validar)
const validar = () => {
  const f = formulario.value
  if (!f.fecha) return 'La fecha es obligatoria'
  if (f.fecha < hoy) return 'La fecha del turno no puede ser pasada'
  if (!f.horaInicio) return 'La hora de inicio es obligatoria'
  if (!f.horaFin) return 'La hora de fin es obligatoria'
  if (f.horaFin <= f.horaInicio) return 'La hora de fin debe ser posterior a la hora de inicio'
  if (!f.actividad.trim()) return 'La actividad es obligatoria'
  if (!Number.isInteger(f.cuposTotales) || f.cuposTotales < 1) return 'Los cupos deben ser al menos 1'
  if (f.cuposTotales > 500) return 'Los cupos no pueden superar 500'
  return ''
}

const guardarTurno = async () => {
  error.value = validar()
  if (error.value) return

  cargando.value = true
  try {
    await turnoService.crearTurno({
      fecha: formulario.value.fecha,
      horaInicio: formulario.value.horaInicio,
      horaFin: formulario.value.horaFin,
      actividad: formulario.value.actividad.trim(),
      cuposTotales: formulario.value.cuposTotales
    })
    router.push('/turnos')
  } catch (err) {
    error.value = err.message
  } finally {
    cargando.value = false
  }
}

const volver = () => router.push('/turnos')
</script>

<style scoped>
.turno-form {
  min-height: calc(100vh - 94px);
  padding: 60px 24px;
  background: var(--crema-claro);
}

.turno-contenedor {
  width: min(600px, 100%);
  margin: auto;
  padding: 55px min(9%, 70px);
  box-sizing: border-box;
  background: white;
  border-radius: 30px;
  box-shadow: 0 20px 60px rgba(81, 72, 62, 0.07);
}

.turno-etiqueta {
  font-size: 10px;
  letter-spacing: 2.5px;
  font-weight: 700;
  color: var(--verde-oscuro);
}

.turno-contenedor h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-size: clamp(34px, 3.5vw, 48px);
  font-weight: 400;
  color: var(--texto);
}

.turno-contenedor h1 em { color: var(--verde-oscuro); }

.turno-descripcion {
  color: var(--texto-suave);
  font-size: 13px;
  line-height: 1.8;
}

.turno-formulario {
  display: flex;
  flex-direction: column;
  gap: 22px;
  margin-top: 32px;
}

.turno-fila {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.turno-campo {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.turno-campo label {
  color: var(--texto);
  font-size: 12px;
  font-weight: 700;
}

.turno-campo input {
  width: 100%;
  box-sizing: border-box;
  padding: 15px 17px;
  background: var(--crema-claro);
  border: 1px solid var(--crema);
  border-radius: 12px;
  outline: none;
  color: var(--texto);
  font-size: 13px;
  font-family: inherit;
  transition: border-color 0.2s;
}

.turno-campo input:focus { border-color: var(--salvia); }

.turno-error {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 14px 17px;
  border: 1px solid #E9B0A5;
  border-radius: 12px;
  background: #FFF0EB;
  color: #A34D41;
  font-size: 12px;
  line-height: 1.5;
}

.turno-boton {
  width: 100%;
  padding: 16px 24px;
  border: none;
  border-radius: 30px;
  background: var(--verde-oscuro);
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, background 0.2s;
}

.turno-boton:hover { transform: translateY(-2px); background: var(--salvia); }
.turno-boton:disabled { opacity: 0.6; cursor: not-allowed; }

.turno-volver {
  display: block;
  margin: 26px auto 0;
  border: none;
  background: transparent;
  color: var(--texto-suave);
  font-size: 12px;
  cursor: pointer;
}

.turno-volver:hover { color: var(--verde-oscuro); }

@media (max-width: 600px) {
  .turno-contenedor { padding: 38px 24px; }
  .turno-fila { grid-template-columns: 1fr; }
}
</style>
