<template>
  <section class="vols-pagina">
    <div class="vols-contenedor">
      <div class="vols-encabezado">
        <span class="vols-etiqueta">REPORTES</span>
        <h1>Reportes de <em>voluntarios, tareas y turnos.</em></h1>
        <p>
          Consulta el resumen y el detalle de voluntarios, tareas o turnos, y expórtalo
          a PDF o Excel para archivarlo o compartirlo.
        </p>
      </div>

      <div class="vols-tarjeta">
        <div class="selector-reporte">
          <button
            v-for="tipo in TIPOS_REPORTE"
            :key="tipo.valor"
            class="vols-boton-editar"
            :class="{ 'boton-activo': tipoSeleccionado === tipo.valor }"
            @click="seleccionar(tipo.valor)"
          >
            {{ tipo.etiqueta }}
          </button>
        </div>

        <div v-if="cargando" class="vols-mensaje">Cargando reporte...</div>
        <div v-else-if="error" class="vols-mensaje vols-mensaje-error" role="alert">{{ error }}</div>

        <template v-else-if="reporte">
          <div class="encabezado-reporte">
            <h2 class="titulo-reporte">{{ reporte.titulo }}</h2>
            <div class="acciones-exportar">
              <button
                class="vols-boton-editar"
                :disabled="exportando === 'pdf'"
                @click="exportar('pdf')"
              >
                {{ exportando === 'pdf' ? 'Exportando...' : 'Exportar PDF' }}
              </button>
              <button
                class="vols-boton-editar"
                :disabled="exportando === 'excel'"
                @click="exportar('excel')"
              >
                {{ exportando === 'excel' ? 'Exportando...' : 'Exportar Excel' }}
              </button>
            </div>
          </div>

          <div v-if="errorExportar" class="vols-mensaje vols-mensaje-error" role="alert">{{ errorExportar }}</div>

          <div class="resumen-tarjetas">
            <div v-for="(valor, etiqueta) in reporte.resumen" :key="etiqueta" class="resumen-tarjeta">
              <span class="resumen-valor">{{ valor }}</span>
              <span class="resumen-etiqueta">{{ etiqueta }}</span>
            </div>
          </div>

          <div v-if="reporte.filas.length === 0" class="vols-mensaje">
            No hay datos para este reporte.
          </div>

          <div v-else class="vols-tabla-contenedor">
            <table class="vols-tabla">
              <thead>
                <tr>
                  <th v-for="encabezado in reporte.encabezados" :key="encabezado">{{ encabezado }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(fila, indiceFila) in reporte.filas" :key="indiceFila">
                  <td v-for="(celda, indiceCelda) in fila" :key="indiceCelda">{{ formatearCelda(celda) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reporteService, TIPOS_REPORTE } from '../services/reporteService'

const tipoSeleccionado = ref(TIPOS_REPORTE[0].valor)
const reporte = ref(null)
const cargando = ref(true)
const error = ref('')
const exportando = ref('')
const errorExportar = ref('')

const cargarReporte = async (tipo) => {
  cargando.value = true
  error.value = ''
  try {
    reporte.value = await reporteService.obtenerReporte(tipo)
  } catch (err) {
    error.value = err.message
    reporte.value = null
  } finally {
    cargando.value = false
  }
}

const seleccionar = (tipo) => {
  tipoSeleccionado.value = tipo
  errorExportar.value = ''
  cargarReporte(tipo)
}

const exportar = async (formato) => {
  exportando.value = formato
  errorExportar.value = ''
  try {
    if (formato === 'pdf') {
      await reporteService.exportarPdf(tipoSeleccionado.value)
    } else {
      await reporteService.exportarExcel(tipoSeleccionado.value)
    }
  } catch (err) {
    errorExportar.value = err.message
  } finally {
    exportando.value = ''
  }
}

const formatearCelda = (valor) => {
  if (typeof valor === 'boolean') return valor ? 'Sí' : 'No'
  if (valor === null || valor === undefined || valor === '') return '—'
  return valor
}

onMounted(() => cargarReporte(tipoSeleccionado.value))
</script>

<style scoped>
/* Reutilizando estilos base del sistema */
.vols-pagina {
  min-height: calc(100vh - 94px);
  padding: 75px 24px 100px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}
.vols-contenedor {
  width: min(1000px, 100%);
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
  font-size: clamp(36px, 5vw, 58px);
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

.selector-reporte {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 28px;
}
.vols-boton-editar {
  padding: 10px 20px;
  border: 1px solid var(--salvia-claro);
  border-radius: 10px;
  background: transparent;
  color: var(--texto);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.vols-boton-editar:hover:not(:disabled) {
  background: var(--crema);
}
.vols-boton-editar:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.boton-activo {
  background: var(--verde-oscuro);
  color: white;
  border-color: var(--verde-oscuro);
}
.boton-activo:hover {
  background: var(--verde-oscuro);
}

.encabezado-reporte {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}
.titulo-reporte {
  font-family: "DM Serif Display", serif;
  font-size: 24px;
  font-weight: 400;
  color: var(--texto);
  margin: 0;
}
.acciones-exportar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.resumen-tarjetas {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 28px;
}
.resumen-tarjeta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 24px;
  background: var(--crema-claro);
  border-radius: 14px;
  min-width: 120px;
}
.resumen-valor {
  font-family: "DM Serif Display", serif;
  font-size: 28px;
  color: var(--verde-oscuro);
}
.resumen-etiqueta {
  font-size: 11px;
  color: var(--texto-suave);
  font-weight: 600;
}

.vols-mensaje {
  text-align: center;
  padding: 40px;
  color: var(--texto-suave);
  font-size: 14px;
}
.vols-mensaje-error {
  color: #A34D41;
  text-align: left;
  padding: 12px;
  background: #FFF0EB;
  border-radius: 10px;
  border: 1px solid #E9B0A5;
  margin-bottom: 20px;
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
  padding: 16px;
  border-bottom: 1px solid var(--crema);
  vertical-align: middle;
  font-size: 13px;
  color: var(--texto);
}
</style>
