<template>
  <section class="vol-form">
    <div class="vol-contenedor">
      <div class="vol-formulario-contenedor">
        <span class="vol-etiqueta">{{ esEdicion ? 'MODIFICAR REGISTRO' : 'NUEVO REGISTRO' }}</span>
        <h1>
          {{ esEdicion ? 'Editar' : 'Registrar' }}
          <em>voluntario.</em>
        </h1>
        <p class="vol-descripcion">
          Completa los datos personales y de contacto. Los voluntarios nuevos quedan registrados como activos.
        </p>

        <form class="vol-formulario" novalidate @submit.prevent="guardarVoluntario">
          <div class="vol-campo">
            <label for="nombre">Nombre *</label>
            <input id="nombre" type="text" v-model="formulario.nombre" placeholder="Ej: Ana" maxlength="100" />
          </div>

          <div class="vol-campo">
            <label for="apellido">Apellido *</label>
            <input id="apellido" type="text" v-model="formulario.apellido" placeholder="Ej: Quispe" maxlength="100" />
          </div>

          <div class="vol-campo">
            <label for="documento">Documento de identidad *</label>
            <input id="documento" type="text" v-model="formulario.documento" placeholder="Ej: 1234567" maxlength="30" />
          </div>

          <div class="vol-campo">
            <label for="email">Correo electrónico *</label>
            <input id="email" type="email" v-model="formulario.email" placeholder="ana@correo.com" maxlength="150" />
          </div>

          <div class="vol-campo">
            <label for="telefono">Teléfono</label>
            <input id="telefono" type="tel" v-model="formulario.telefono" placeholder="Ej: 70012345" maxlength="20" />
          </div>

          <div v-if="error" class="vol-error" role="alert">
            <span aria-hidden="true">⚠</span>
            {{ error }}
          </div>

          <button type="submit" class="vol-boton" :disabled="cargando">
            {{ cargando ? 'Guardando...' : (esEdicion ? 'Guardar Cambios' : 'Registrar Voluntario') }}
          </button>
        </form>

        <button type="button" class="vol-volver" @click="volver">← Volver a la lista</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { voluntarioService } from '../services/voluntarioService'

const router = useRouter()
const route = useRoute()

const esEdicion = ref(!!route.params.id)
const cargando = ref(false)
const error = ref('')

const formulario = ref({
  nombre: '',
  apellido: '',
  documento: '',
  email: '',
  telefono: ''
})

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const TELEFONO_REGEX = /^[0-9+()\-\s]{6,20}$/

// Criterio 4: validaciones del lado del cliente (el backend vuelve a validar)
const validar = () => {
  const f = formulario.value
  if (!f.nombre.trim()) return 'El nombre es obligatorio'
  if (!f.apellido.trim()) return 'El apellido es obligatorio'
  if (!f.documento.trim()) return 'El documento es obligatorio'
  if (!f.email.trim()) return 'El correo es obligatorio'
  if (!EMAIL_REGEX.test(f.email.trim())) return 'El correo no tiene un formato válido'
  if (f.telefono.trim() && !TELEFONO_REGEX.test(f.telefono.trim())) return 'El teléfono no tiene un formato válido'
  return ''
}

onMounted(async () => {
  if (!esEdicion.value) return
  try {
    const v = await voluntarioService.obtenerVoluntario(route.params.id)
    formulario.value = {
      nombre: v.nombre,
      apellido: v.apellido,
      documento: v.documento,
      email: v.email,
      telefono: v.telefono || ''
    }
  } catch (err) {
    error.value = 'Error al cargar los datos: ' + err.message
  }
})

const guardarVoluntario = async () => {
  error.value = validar()
  if (error.value) return

  cargando.value = true
  try {
    const payload = {
      nombre: formulario.value.nombre.trim(),
      apellido: formulario.value.apellido.trim(),
      documento: formulario.value.documento.trim(),
      email: formulario.value.email.trim(),
      telefono: formulario.value.telefono.trim()
    }

    if (esEdicion.value) {
      await voluntarioService.actualizarVoluntario(route.params.id, payload)
    } else {
      await voluntarioService.registrarVoluntario(payload)
    }

    router.push('/voluntarios')
  } catch (err) {
    // Criterio 2: mensaje claro ante documento o correo duplicado
    error.value = err.message
  } finally {
    cargando.value = false
  }
}

const volver = () => {
  router.push('/voluntarios')
}
</script>

<style scoped>
.vol-form {
  min-height: calc(100vh - 94px);
  padding: 60px 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--crema-claro);
}

.vol-contenedor {
  width: min(600px, 100%);
  background: white;
  border-radius: 30px;
  box-shadow: 0 20px 60px rgba(81, 72, 62, 0.07);
  overflow: hidden;
}

.vol-formulario-contenedor {
  padding: 65px min(9%, 75px);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.vol-etiqueta {
  font-size: 10px;
  letter-spacing: 2.5px;
  font-weight: 700;
  color: var(--verde-oscuro);
}

.vol-formulario-contenedor h1 {
  margin: 18px 0;
  font-family: "DM Serif Display", serif;
  font-size: clamp(34px, 3.5vw, 48px);
  font-weight: 400;
  line-height: 1.2;
  color: var(--texto);
}

.vol-formulario-contenedor h1 em {
  color: var(--verde-oscuro);
}

.vol-descripcion {
  color: var(--texto-suave);
  font-size: 13px;
  line-height: 1.8;
}

.vol-formulario {
  display: flex;
  flex-direction: column;
  gap: 23px;
  margin-top: 35px;
}

.vol-campo {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.vol-campo label {
  color: var(--texto);
  font-size: 12px;
  font-weight: 700;
}

.vol-campo input {
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

.vol-campo input:focus {
  border-color: var(--salvia);
}

.vol-boton {
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

.vol-boton:hover {
  transform: translateY(-2px);
  background: var(--salvia);
}

.vol-boton:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.vol-error {
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

.vol-volver {
  align-self: center;
  margin-top: 28px;
  border: none;
  background: transparent;
  color: var(--texto-suave);
  font-size: 12px;
  cursor: pointer;
}

.vol-volver:hover {
  color: var(--verde-oscuro);
}

@media (max-width: 750px) {
  .vol-formulario-contenedor { padding: 42px 28px; }
}
</style>
