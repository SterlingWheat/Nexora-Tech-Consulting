<template>
  <section class="vols-pagina">
    <div class="vols-contenedor">
      <div class="vols-encabezado">
        <span class="vols-etiqueta">REPORTES</span>
        <h1>Exportación de <em>reportes.</em></h1>
        <p>
          Descarga el reporte de voluntarios, tareas o turnos en PDF o Excel para
          archivarlo o compartirlo.
        </p>
      </div>

      <div class="vols-tarjeta">
        <div class="reportes-lista">
          <div v-for="reporte in TIPOS_REPORTE" :key="reporte.valor" class="reporte-fila">
            <div>
              <h2>{{ reporte.etiqueta }}</h2>
              <p>Reporte de {{ reporte.etiqueta.toLowerCase() }}.</p>
            </div>
            <div class="reporte-acciones">
              <button
                class="vols-boton-editar"
                :disabled="exportando === reporte.valor + '-pdf'"
                @click="exportar(reporte.valor, 'pdf')"
              >
                {{ exportando === reporte.valor + '-pdf' ? 'Exportando...' : 'Exportar PDF' }}
              </button>
              <button
                class="vols-boton-editar"
                :disabled="exportando === reporte.valor + '-excel'"
                @click="exportar(reporte.valor, 'excel')"
              >
                {{ exportando === reporte.valor + '-excel' ? 'Exportando...' : 'Exportar Excel' }}
              </button>
            </div>
          </div>
        </div>

        <div v-if="error" class="vols-mensaje vols-mensaje-error" role="alert">{{ error }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { reporteService, TIPOS_REPORTE } from '../services/reporteService'

const exportando = ref('')
const error = ref('')

const exportar = async (tipo, formato) => {
  exportando.value = `${tipo}-${formato}`
  error.value = ''
  try {
    if (formato === 'pdf') {
      await reporteService.exportarPdf(tipo)
    } else {
      await reporteService.exportarExcel(tipo)
    }
  } catch (err) {
    error.value = err.message
  } finally {
    exportando.value = ''
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

.reportes-lista {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.reporte-fila {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px;
  border: 1px solid var(--crema);
  border-radius: 16px;
}
.reporte-fila h2 {
  font-family: "DM Serif Display", serif;
  font-size: 22px;
  font-weight: 400;
  color: var(--texto);
  margin: 0;
}
.reporte-fila p {
  margin-top: 6px;
  font-size: 12px;
  color: var(--texto-suave);
}
.reporte-acciones {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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

.vols-mensaje-error {
  margin-top: 20px;
  padding: 12px;
  background: #FFF0EB;
  color: #A34D41;
  border-radius: 10px;
  font-size: 12px;
  border: 1px solid #E9B0A5;
}
</style>
