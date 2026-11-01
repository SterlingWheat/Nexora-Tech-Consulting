<template>
  <section class="plantillas-pagina">
    <div class="plantillas-contenedor">
      <span class="plantillas-etiqueta">ADMINISTRACIÓN</span>
      <h1>Plantillas de <em>notificación.</em></h1>
      <p class="plantillas-intro">
        Edita las plantillas persistidas que usa el sistema para asignaciones, recordatorios y cambios de turno.
        Variables disponibles: <code v-text="'{{nombre}}'"></code>, <code v-text="'{{actividad}}'"></code> y
        <code v-text="'{{fecha}}'"></code>.
      </p>

      <p v-if="cargando" class="plantillas-mensaje">Cargando plantillas...</p>
      <p v-else-if="errorCarga" class="plantillas-mensaje error" role="alert">{{ errorCarga }}</p>
      <template v-else>
        <div class="plantillas-layout">
          <nav class="plantillas-lista" aria-label="Plantillas disponibles">
            <button
              v-for="plantilla in plantillas"
              :key="plantilla.templateType"
              type="button"
              :class="{ seleccionada: plantilla.templateType === seleccionada }"
              @click="seleccionar(plantilla.templateType)"
            >
              <span>{{ plantilla.name }}</span>
              <small>{{ plantilla.active ? 'Activa' : 'Inactiva' }}</small>
            </button>
          </nav>

          <form v-if="formulario" class="plantillas-editor" @submit.prevent="guardar">
            <div class="plantillas-editor-cabecera">
              <div>
                <h2>{{ plantillaActual.name }}</h2>
                <p>Los cambios se guardan en la base de datos.</p>
              </div>
              <label class="plantillas-activo">
                <input v-model="formulario.active" type="checkbox">
                Activa
              </label>
            </div>

            <label>
              Asunto
              <input v-model="formulario.subject" maxlength="200" required>
            </label>
            <label>
              Contenido HTML
              <textarea v-model="formulario.htmlBody" rows="10" required></textarea>
            </label>
            <label>
              Texto plano
              <textarea v-model="formulario.textBody" rows="7" required></textarea>
            </label>

            <p v-if="mensajeGuardar" class="plantillas-mensaje exito" role="status">{{ mensajeGuardar }}</p>
            <p v-if="errorGuardar" class="plantillas-mensaje error" role="alert">{{ errorGuardar }}</p>
            <button class="plantillas-boton" type="submit" :disabled="guardando">
              {{ guardando ? 'Guardando...' : 'Guardar cambios' }}
            </button>
          </form>
        </div>

        <form v-if="plantillaActual" class="plantillas-envio" @submit.prevent="enviar">
          <div>
            <h2>Enviar con esta plantilla</h2>
            <p>Usa datos reales para enviar la versión guardada mediante el SMTP configurado.</p>
          </div>
          <div class="plantillas-envio-campos">
            <label>
              Correo destinatario
              <input v-model="destinatario.recipient" type="email" required maxlength="254">
            </label>
            <label>
              Nombre
              <input v-model="destinatario.name" required maxlength="150">
            </label>
            <label>
              Actividad
              <input v-model="destinatario.activity" required maxlength="500">
            </label>
            <label>
              Fecha
              <input v-model="destinatario.date" required maxlength="100">
            </label>
          </div>
          <p v-if="mensajeEnvio" class="plantillas-mensaje exito" role="status">{{ mensajeEnvio }}</p>
          <p v-if="errorEnvio" class="plantillas-mensaje error" role="alert">{{ errorEnvio }}</p>
          <button
            class="plantillas-boton secundario"
            type="submit"
            :disabled="enviando || !plantillaActual.active || hayCambiosSinGuardar"
          >
            {{ enviando ? 'Enviando...' : 'Enviar correo' }}
          </button>
          <p v-if="hayCambiosSinGuardar" class="plantillas-ayuda">
            Guarda los cambios antes de enviar para usar la versión actualizada.
          </p>
          <p v-if="!plantillaActual.active" class="plantillas-ayuda">
            Activa la plantilla y guarda los cambios antes de enviar.
          </p>
        </form>
      </template>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { notificationTemplateService } from '../services/notificationTemplateService'

const plantillas = ref([])
const seleccionada = ref('')
const cargando = ref(true)
const errorCarga = ref('')
const guardando = ref(false)
const enviando = ref(false)
const errorGuardar = ref('')
const mensajeGuardar = ref('')
const errorEnvio = ref('')
const mensajeEnvio = ref('')
const formulario = reactive({ subject: '', htmlBody: '', textBody: '', active: true })
const destinatario = reactive({ recipient: '', name: '', activity: '', date: '' })
const plantillaActual = computed(() =>
  plantillas.value.find((plantilla) => plantilla.templateType === seleccionada.value)
)
const hayCambiosSinGuardar = computed(() => {
  if (!plantillaActual.value) return false
  return formulario.subject !== plantillaActual.value.subject
    || formulario.htmlBody !== plantillaActual.value.htmlBody
    || formulario.textBody !== plantillaActual.value.textBody
    || formulario.active !== plantillaActual.value.active
})

const copiarAlFormulario = (plantilla) => {
  formulario.subject = plantilla.subject
  formulario.htmlBody = plantilla.htmlBody
  formulario.textBody = plantilla.textBody
  formulario.active = plantilla.active
}

const seleccionar = (tipo) => {
  seleccionada.value = tipo
  const plantilla = plantillas.value.find((item) => item.templateType === tipo)
  if (plantilla) copiarAlFormulario(plantilla)
  errorGuardar.value = ''
  mensajeGuardar.value = ''
  errorEnvio.value = ''
  mensajeEnvio.value = ''
}

const cargar = async () => {
  cargando.value = true
  errorCarga.value = ''
  try {
    plantillas.value = await notificationTemplateService.listar()
    if (plantillas.value.length) seleccionar(plantillas.value[0].templateType)
  } catch (error) {
    errorCarga.value = error.message || 'No se pudieron cargar las plantillas.'
  } finally {
    cargando.value = false
  }
}

const guardar = async () => {
  guardando.value = true
  errorGuardar.value = ''
  mensajeGuardar.value = ''
  try {
    const guardada = await notificationTemplateService.actualizar(seleccionada.value, {
      subject: formulario.subject,
      htmlBody: formulario.htmlBody,
      textBody: formulario.textBody,
      active: formulario.active
    })
    const index = plantillas.value.findIndex((item) => item.templateType === guardada.templateType)
    if (index >= 0) plantillas.value[index] = guardada
    copiarAlFormulario(guardada)
    mensajeGuardar.value = 'Plantilla guardada. Los siguientes envíos usarán esta versión.'
  } catch (error) {
    errorGuardar.value = error.message || 'No se pudo guardar la plantilla.'
  } finally {
    guardando.value = false
  }
}

const enviar = async () => {
  enviando.value = true
  errorEnvio.value = ''
  mensajeEnvio.value = ''
  try {
    await notificationTemplateService.enviar(seleccionada.value, { ...destinatario })
    mensajeEnvio.value = 'El servidor SMTP aceptó el mensaje para envío.'
  } catch (error) {
    errorEnvio.value = error.message || 'No se pudo enviar el correo.'
  } finally {
    enviando.value = false
  }
}

onMounted(cargar)
</script>

<style scoped>
.plantillas-pagina {
  min-height: calc(100vh - 94px);
  padding: 64px 24px 90px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}

.plantillas-contenedor { width: min(1120px, 100%); margin: auto; }
.plantillas-etiqueta { font-size: 10px; font-weight: 700; color: var(--verde-oscuro); letter-spacing: 2px; }
.plantillas-contenedor h1 { margin: 18px 0; font-family: "DM Serif Display", serif; font-weight: 400; font-size: clamp(38px, 5vw, 58px); }
.plantillas-contenedor h1 em { color: var(--verde-oscuro); }
.plantillas-intro { max-width: 760px; margin-bottom: 30px; color: var(--texto-suave); line-height: 1.8; }
.plantillas-intro code { padding: 2px 5px; background: #f3eee7; border-radius: 4px; color: var(--verde-oscuro); }
.plantillas-layout { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 20px; align-items: start; }
.plantillas-lista, .plantillas-editor, .plantillas-envio { padding: 22px; background: white; border: 1px solid var(--crema); border-radius: 18px; }
.plantillas-lista { display: grid; gap: 8px; }
.plantillas-lista button { display: flex; justify-content: space-between; gap: 10px; padding: 12px; border: 0; border-radius: 10px; background: transparent; color: var(--texto); text-align: left; cursor: pointer; }
.plantillas-lista button.seleccionada { background: var(--crema-claro); color: var(--verde-oscuro); font-weight: 700; }
.plantillas-lista small { color: var(--texto-suave); white-space: nowrap; }
.plantillas-editor, .plantillas-envio { display: grid; gap: 16px; }
.plantillas-editor-cabecera { display: flex; justify-content: space-between; align-items: center; gap: 15px; }
.plantillas-editor h2, .plantillas-envio h2 { margin: 0 0 6px; font-family: "DM Serif Display", serif; font-weight: 400; font-size: 25px; }
.plantillas-editor-cabecera p, .plantillas-envio p { margin: 0; color: var(--texto-suave); font-size: 13px; line-height: 1.6; }
.plantillas-editor label, .plantillas-envio label { display: grid; gap: 7px; color: var(--texto); font-size: 13px; font-weight: 600; }
.plantillas-editor input:not([type="checkbox"]), .plantillas-editor textarea, .plantillas-envio input { width: 100%; box-sizing: border-box; padding: 11px 12px; border: 1px solid var(--crema); border-radius: 8px; color: var(--texto); font: inherit; }
.plantillas-editor textarea { resize: vertical; font-family: Consolas, monospace; font-size: 12px; line-height: 1.5; }
.plantillas-activo { display: flex !important; align-items: center; gap: 8px !important; white-space: nowrap; }
.plantillas-boton { justify-self: start; padding: 11px 20px; border: 0; border-radius: 24px; background: var(--verde-oscuro); color: white; font-weight: 700; cursor: pointer; }
.plantillas-boton:disabled { opacity: 0.6; cursor: wait; }
.plantillas-boton.secundario { background: #8d745e; }
.plantillas-envio { margin-top: 20px; }
.plantillas-envio-campos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.plantillas-mensaje { margin: 0; padding: 12px; border-radius: 8px; background: #f3eee7; color: var(--texto-suave); }
.plantillas-mensaje.error { background: #fff0eb; color: #a34d41; }
.plantillas-mensaje.exito { background: #e8f2e5; color: #50704b; }
.plantillas-ayuda { color: var(--texto-suave); font-size: 12px; }

@media (max-width: 760px) {
  .plantillas-pagina { padding: 45px 15px; }
  .plantillas-layout { grid-template-columns: 1fr; }
  .plantillas-lista { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .plantillas-lista button { display: grid; }
}

@media (max-width: 560px) {
  .plantillas-envio-campos { grid-template-columns: 1fr; }
}
</style>
