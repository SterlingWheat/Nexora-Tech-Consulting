<template>
  <section class="disp-pagina">
    <div class="disp-contenedor">
      <span class="disp-etiqueta">DISPONIBILIDAD HORARIA</span>
      <h1>Días y <em>franjas disponibles.</em></h1>
      <p class="disp-descripcion">
        <template v-if="voluntario">{{ voluntario.nombre }} {{ voluntario.apellido }} — </template>
        Registra los días y horarios en que el voluntario puede ser asignado.
      </p>

      <div v-if="cargando" class="disp-mensaje">Cargando disponibilidad...</div>

      <form v-else class="disp-formulario" novalidate @submit.prevent="guardar">
        <div v-for="dia in DIAS" :key="dia.valor" class="disp-dia">
          <div class="disp-dia-encabezado">
            <strong>{{ dia.etiqueta }}</strong>
            <button type="button" class="disp-agregar" @click="agregarFranja(dia.valor)">+ Agregar franja</button>
          </div>

          <p v-if="franjasDe(dia.valor).length === 0" class="disp-sin-franjas">Sin disponibilidad</p>

          <div v-for="(franja, i) in franjasDe(dia.valor)" :key="franja.uid" class="disp-franja">
            <input
              type="time"
              v-model="franja.horaInicio"
              :aria-label="`Hora de inicio ${dia.etiqueta} ${i + 1}`"
            />
            <span aria-hidden="true">a</span>
            <input
              type="time"
              v-model="franja.horaFin"
              :aria-label="`Hora de fin ${dia.etiqueta} ${i + 1}`"
            />
            <button type="button" class="disp-quitar" :aria-label="`Quitar franja ${dia.etiqueta} ${i + 1}`" @click="quitarFranja(franja.uid)">✕</button>
          </div>
        </div>

        <div v-if="error" class="disp-error" role="alert">
          <span aria-hidden="true">⚠</span> {{ error }}
        </div>

        <div v-if="advertencias.length" class="disp-aviso" role="status">
          <span aria-hidden="true">ℹ</span>
          <ul>
            <li v-for="a in advertencias" :key="a">{{ a }}</li>
          </ul>
        </div>

        <div v-if="guardado && !error" class="disp-exito" role="status">Disponibilidad guardada en el perfil.</div>

        <button type="submit" class="disp-boton" :disabled="guardando">
          {{ guardando ? 'Guardando...' : 'Guardar disponibilidad' }}
        </button>
      </form>

      <button type="button" class="disp-volver" @click="volver">← Volver a voluntarios</button>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { disponibilidadService, DIAS } from '../services/disponibilidadService'
import { voluntarioService } from '../services/voluntarioService'

const router = useRouter()
const route = useRoute()
const idVoluntario = route.params.id

const voluntario = ref(null)
const franjas = ref([])
const cargando = ref(true)
const guardando = ref(false)
const guardado = ref(false)
const error = ref('')
const advertencias = ref([])

let siguienteUid = 1
const conUid = (f) => ({ uid: siguienteUid++, diaSemana: f.diaSemana, horaInicio: recortar(f.horaInicio), horaFin: recortar(f.horaFin) })
// El backend devuelve HH:mm:ss; los inputs type="time" trabajan con HH:mm
const recortar = (hora) => (hora ? hora.slice(0, 5) : '')

const franjasDe = (dia) => franjas.value.filter((f) => f.diaSemana === dia)

const agregarFranja = (dia) => {
  guardado.value = false
  franjas.value.push(conUid({ diaSemana: dia, horaInicio: '08:00', horaFin: '12:00' }))
}

const quitarFranja = (uid) => {
  guardado.value = false
  franjas.value = franjas.value.filter((f) => f.uid !== uid)
}

// Criterio 2: validación en cliente (el backend vuelve a validar)
const validar = () => {
  for (const f of franjas.value) {
    const etiqueta = DIAS.find((d) => d.valor === f.diaSemana).etiqueta
    if (!f.horaInicio || !f.horaFin) return `Completa la hora de inicio y fin del ${etiqueta}`
    if (f.horaFin <= f.horaInicio) return `En el ${etiqueta}, la hora de fin debe ser posterior a la hora de inicio`
  }
  return ''
}

onMounted(async () => {
  try {
    const [v, disp] = await Promise.all([
      voluntarioService.obtenerVoluntario(idVoluntario),
      disponibilidadService.obtener(idVoluntario),
    ])
    voluntario.value = v
    franjas.value = disp.franjas.map(conUid)
  } catch (err) {
    error.value = err.message
  } finally {
    cargando.value = false
  }
})

const guardar = async () => {
  guardado.value = false
  advertencias.value = []
  error.value = validar()
  if (error.value) return

  guardando.value = true
  try {
    const payload = franjas.value.map(({ diaSemana, horaInicio, horaFin }) => ({ diaSemana, horaInicio, horaFin }))
    const respuesta = await disponibilidadService.guardar(idVoluntario, payload)
    // Criterio 3: se muestran las franjas unificadas y el aviso
    franjas.value = respuesta.franjas.map(conUid)
    advertencias.value = respuesta.advertencias || []
    guardado.value = true
  } catch (err) {
    error.value = err.message
  } finally {
    guardando.value = false
  }
}

const volver = () => router.push('/voluntarios')
</script>

<style scoped>
.disp-pagina {
  min-height: calc(100vh - 94px);
  padding: 60px 24px 90px;
  background: var(--crema-claro);
}

.disp-contenedor {
  width: min(720px, 100%);
  margin: auto;
  padding: 50px min(8%, 60px);
  box-sizing: border-box;
  background: white;
  border-radius: 30px;
  box-shadow: 0 20px 60px rgba(81, 72, 62, 0.07);
}

.disp-etiqueta {
  font-size: 10px;
  letter-spacing: 2.5px;
  font-weight: 700;
  color: var(--verde-oscuro);
}

.disp-contenedor h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-size: clamp(32px, 3.5vw, 44px);
  font-weight: 400;
  color: var(--texto);
}

.disp-contenedor h1 em { color: var(--verde-oscuro); }

.disp-descripcion {
  color: var(--texto-suave);
  font-size: 13px;
  line-height: 1.8;
}

.disp-mensaje { padding: 40px; text-align: center; color: var(--texto-suave); }

.disp-formulario {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 30px;
}

.disp-dia {
  padding: 16px 18px;
  background: var(--crema-claro);
  border: 1px solid var(--crema);
  border-radius: 14px;
}

.disp-dia-encabezado {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--texto);
}

.disp-agregar {
  border: none;
  background: transparent;
  color: var(--verde-oscuro);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.disp-sin-franjas {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--texto-suave);
}

.disp-franja {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--texto-suave);
}

.disp-franja input {
  padding: 9px 12px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 10px;
  color: var(--texto);
  font-family: inherit;
  font-size: 13px;
}

.disp-quitar {
  margin-left: auto;
  border: none;
  background: transparent;
  color: #A34D41;
  cursor: pointer;
}

.disp-error, .disp-aviso, .disp-exito {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 14px 17px;
  border-radius: 12px;
  font-size: 12px;
  line-height: 1.5;
}

.disp-error { border: 1px solid #E9B0A5; background: #FFF0EB; color: #A34D41; }
.disp-aviso { border: 1px solid var(--durazno); background: #FFF7EE; color: #8A5A36; }
.disp-aviso ul { margin: 0; padding-left: 16px; }
.disp-exito { border: 1px solid var(--salvia-claro); background: #F1F5EC; color: #50704B; }

.disp-boton {
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

.disp-boton:hover { transform: translateY(-2px); background: var(--salvia); }
.disp-boton:disabled { opacity: 0.6; cursor: not-allowed; }

.disp-volver {
  display: block;
  margin: 26px auto 0;
  border: none;
  background: transparent;
  color: var(--texto-suave);
  font-size: 12px;
  cursor: pointer;
}

.disp-volver:hover { color: var(--verde-oscuro); }

@media (max-width: 600px) {
  .disp-contenedor { padding: 36px 20px; }
  .disp-franja { flex-wrap: wrap; }
}
</style>
