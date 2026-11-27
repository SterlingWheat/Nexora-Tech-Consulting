<template>
  <section class="dash-pagina">
    <div class="dash-contenedor">
      <span class="dash-etiqueta">PANEL ADMINISTRATIVO</span>
      <h1>Supervisa la <em>operación.</em></h1>
      <p class="dash-descripcion">Indicadores clave de voluntarios, tareas, turnos y asignaciones, de un vistazo.</p>

      <div v-if="error" class="dash-error" role="alert"><span aria-hidden="true">⚠</span> {{ error }}</div>

      <div v-if="cargando" class="dash-mensaje">Cargando panel...</div>

      <template v-else-if="metricas">
        <!-- Criterio 1: voluntarios activos, tareas abiertas, turnos próximos y asignaciones pendientes -->
        <!-- Criterio 2: cada tarjeta navega al listado filtrado correspondiente -->
        <div class="dash-tarjetas">
          <button type="button" class="dash-kpi" @click="irA('/voluntarios', { estado: 'activos' })">
            <span class="dash-kpi-icono" aria-hidden="true">🧑‍🤝‍🧑</span>
            <span class="dash-kpi-numero">{{ metricas.voluntariosActivos }}</span>
            <span class="dash-kpi-etiqueta">Voluntarios activos</span>
          </button>

          <button type="button" class="dash-kpi" @click="irA('/tareas', { vista: 'abiertas' })">
            <span class="dash-kpi-icono" aria-hidden="true">📋</span>
            <span class="dash-kpi-numero">{{ metricas.tareasAbiertas }}</span>
            <span class="dash-kpi-etiqueta">Tareas abiertas</span>
          </button>

          <button type="button" class="dash-kpi" @click="irA('/turnos/calendario')">
            <span class="dash-kpi-icono" aria-hidden="true">📅</span>
            <span class="dash-kpi-numero">{{ metricas.turnosProximos }}</span>
            <span class="dash-kpi-etiqueta">Turnos próximos</span>
          </button>

          <button type="button" class="dash-kpi" @click="irA('/tareas')">
            <span class="dash-kpi-icono" aria-hidden="true">⏳</span>
            <span class="dash-kpi-numero">{{ metricas.asignacionesPendientes }}</span>
            <span class="dash-kpi-etiqueta">Asignaciones pendientes</span>
          </button>
        </div>

        <div class="dash-graficos">
          <div class="dash-grafico-tarjeta">
            <span class="dash-grafico-titulo">Voluntarios</span>
            <div class="dash-barra-apilada">
              <div
                class="dash-segmento dash-segmento-activo"
                :style="{ width: porcentaje(metricas.voluntariosActivos, totalVoluntarios) + '%' }"
              ></div>
              <div
                class="dash-segmento dash-segmento-inactivo"
                :style="{ width: porcentaje(metricas.voluntariosInactivos, totalVoluntarios) + '%' }"
              ></div>
            </div>
            <div class="dash-leyenda">
              <span class="dash-leyenda-item"><i class="dash-punto dash-punto-activo"></i> Activos ({{ metricas.voluntariosActivos }})</span>
              <span class="dash-leyenda-item"><i class="dash-punto dash-punto-inactivo"></i> Inactivos ({{ metricas.voluntariosInactivos }})</span>
            </div>
          </div>

          <div class="dash-grafico-tarjeta">
            <span class="dash-grafico-titulo">Tareas por estado</span>
            <div v-if="metricas.tareasPorEstado.length === 0" class="dash-sin-datos">Todavía no hay tareas.</div>
            <div v-else class="dash-barras-horizontales">
              <div v-for="fila in metricas.tareasPorEstado" :key="fila.estado" class="dash-fila-barra">
                <span class="dash-fila-etiqueta">{{ fila.estado }}</span>
                <div class="dash-fila-pista">
                  <div
                    class="dash-fila-relleno"
                    :style="{ width: porcentaje(fila.cantidad, maxTareasPorEstado) + '%' }"
                  ></div>
                </div>
                <span class="dash-fila-numero">{{ fila.cantidad }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardService } from '../services/dashboardService'

const router = useRouter()

const metricas = ref(null)
const cargando = ref(true)
const error = ref('')

const totalVoluntarios = computed(() =>
  metricas.value ? metricas.value.voluntariosActivos + metricas.value.voluntariosInactivos : 0
)

const maxTareasPorEstado = computed(() => {
  if (!metricas.value || metricas.value.tareasPorEstado.length === 0) return 0
  return Math.max(...metricas.value.tareasPorEstado.map((fila) => fila.cantidad))
})

const porcentaje = (valor, total) => (total > 0 ? Math.round((valor / total) * 100) : 0)

// Criterio 2: al hacer clic en un indicador, se navega al listado filtrado correspondiente
const irA = (ruta, query) => router.push({ path: ruta, query })

const cargarMetricas = async () => {
  cargando.value = true
  error.value = ''
  try {
    metricas.value = await dashboardService.obtenerMetricas()
  } catch (err) {
    error.value = err.message
  } finally {
    cargando.value = false
  }
}

onMounted(cargarMetricas)
</script>

<style scoped>
.dash-pagina {
  min-height: calc(100vh - 94px);
  padding: 60px 24px 90px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}

.dash-contenedor {
  width: min(1100px, 100%);
  margin: auto;
}

.dash-etiqueta {
  font-size: 10px;
  letter-spacing: 2.5px;
  font-weight: 700;
  color: var(--verde-oscuro);
}

.dash-contenedor h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-size: clamp(32px, 4vw, 48px);
  font-weight: 400;
  color: var(--texto);
}

.dash-contenedor h1 em { color: var(--verde-oscuro); }

.dash-descripcion {
  color: var(--texto-suave);
  font-size: 13px;
  line-height: 1.8;
  max-width: 620px;
}

.dash-mensaje { padding: 60px; text-align: center; color: var(--texto-suave); }

.dash-error {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  margin-top: 24px;
  padding: 14px 17px;
  border-radius: 12px;
  font-size: 12px;
  line-height: 1.5;
  border: 1px solid #E9B0A5;
  background: #FFF0EB;
  color: #A34D41;
}

.dash-tarjetas {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 32px;
}

.dash-kpi {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 22px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 20px;
  box-shadow: 0 15px 40px rgba(81, 72, 62, 0.05);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: transform 0.2s, box-shadow 0.2s;
}

.dash-kpi:hover {
  transform: translateY(-3px);
  box-shadow: 0 20px 50px rgba(81, 72, 62, 0.1);
}

.dash-kpi-icono { font-size: 22px; }

.dash-kpi-numero {
  font-family: "DM Serif Display", serif;
  font-size: 34px;
  color: var(--texto);
}

.dash-kpi-etiqueta {
  font-size: 12px;
  color: var(--texto-suave);
}

.dash-graficos {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 24px;
}

.dash-grafico-tarjeta {
  padding: 24px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 20px;
  box-shadow: 0 15px 40px rgba(81, 72, 62, 0.05);
}

.dash-grafico-titulo {
  display: block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--verde-oscuro);
  text-transform: uppercase;
  margin-bottom: 16px;
}

.dash-sin-datos { color: var(--texto-suave); font-size: 13px; }

.dash-barra-apilada {
  display: flex;
  width: 100%;
  height: 16px;
  border-radius: 20px;
  overflow: hidden;
  background: var(--crema-claro);
}

.dash-segmento-activo { background: var(--verde-oscuro); }
.dash-segmento-inactivo { background: var(--durazno); }

.dash-leyenda {
  display: flex;
  gap: 18px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--texto-suave);
}

.dash-leyenda-item { display: inline-flex; align-items: center; gap: 6px; }
.dash-punto { width: 9px; height: 9px; border-radius: 50%; display: inline-block; }
.dash-punto-activo { background: var(--verde-oscuro); }
.dash-punto-inactivo { background: var(--durazno); }

.dash-barras-horizontales {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dash-fila-barra {
  display: grid;
  grid-template-columns: 100px 1fr 32px;
  align-items: center;
  gap: 10px;
}

.dash-fila-etiqueta {
  font-size: 12px;
  color: var(--texto);
}

.dash-fila-pista {
  height: 10px;
  border-radius: 10px;
  background: var(--crema-claro);
  overflow: hidden;
}

.dash-fila-relleno {
  height: 100%;
  background: var(--salvia);
  border-radius: 10px;
  transition: width 0.3s;
}

.dash-fila-numero {
  font-size: 12px;
  color: var(--texto-suave);
  text-align: right;
}

@media (max-width: 900px) {
  .dash-tarjetas { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dash-graficos { grid-template-columns: 1fr; }
}

@media (max-width: 560px) {
  .dash-tarjetas { grid-template-columns: 1fr; }
}
</style>
