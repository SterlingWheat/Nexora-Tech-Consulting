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
          Completa los datos personales y de contacto.
          <span v-if="!esEdicion">Opcionalmente, puedes crearle una cuenta para que acceda al sistema.</span>
        </p>

        <form class="vol-formulario" novalidate @submit.prevent="guardarVoluntario">
          <div class="vol-campo">
            <label for="nombre">Nombre *</label>
            <input
              id="nombre"
              v-model="formulario.nombre"
              type="text"
              placeholder="Ej: Ana"
              maxlength="100"
              :class="{ 'input-invalido': erroresCampo.nombre }"
              @input="limpiarErrorCampo('nombre')"
            />
            <span v-if="erroresCampo.nombre" class="vol-campo-error">{{ erroresCampo.nombre }}</span>
          </div>

          <div class="vol-campo">
            <label for="apellido">Apellido *</label>
            <input
              id="apellido"
              ref="apellidoInput"
              v-model="formulario.apellido"
              type="text"
              placeholder="Ej: Quispe"
              maxlength="100"
              :class="{ 'input-invalido': erroresCampo.apellido }"
              @input="limpiarErrorCampo('apellido')"
            />
            <span v-if="erroresCampo.apellido" class="vol-campo-error">{{ erroresCampo.apellido }}</span>
          </div>

          <div class="vol-campo">
            <label for="documento">Documento de identidad *</label>
            <input
              id="documento"
              v-model="formulario.documento"
              type="text"
              placeholder="Ej: 1234567"
              maxlength="30"
              :class="{ 'input-invalido': erroresCampo.documento }"
              @input="limpiarErrorCampo('documento')"
            />
            <span v-if="erroresCampo.documento" class="vol-campo-error">{{ erroresCampo.documento }}</span>
          </div>

          <div class="vol-campo">
            <label for="email">Correo electrónico *</label>
            <input
              id="email"
              v-model="formulario.email"
              type="email"
              placeholder="ana@correo.com"
              maxlength="150"
              :class="{ 'input-invalido': erroresCampo.email }"
              @input="limpiarErrorCampo('email')"
            />
            <span v-if="erroresCampo.email" class="vol-campo-error">{{ erroresCampo.email }}</span>
          </div>

          <div class="vol-campo">
            <label for="telefono">Teléfono</label>
            <input
              id="telefono"
              v-model="formulario.telefono"
              type="tel"
              placeholder="Ej: 70012345"
              maxlength="20"
              :class="{ 'input-invalido': erroresCampo.telefono }"
              @input="limpiarErrorCampo('telefono')"
            />
            <span v-if="erroresCampo.telefono" class="vol-campo-error">{{ erroresCampo.telefono }}</span>
          </div>

          <!-- HU2: Selector de Habilidades -->
          <div class="vol-campo">
            <label>Habilidades / Especialidades</label>
            <div v-if="habilidadesDisponibles.length === 0" class="vol-info-habilidades">
              No hay habilidades en el catálogo.
            </div>
            <div v-else class="vol-habilidades-grid">
              <label v-for="hab in habilidadesDisponibles" :key="hab.id" class="vol-habilidad-item">
                <input
                  type="checkbox"
                  :value="hab.id"
                  v-model="formulario.habilidadesIds"
                />
                <div class="vol-habilidad-texto">
                  <strong>{{ hab.nombre }}</strong>
                  <small v-if="hab.descripcion">{{ hab.descripcion }}</small>
                </div>
              </label>
            </div>
          </div>

          <div v-if="!esEdicion" class="vol-campo-checkbox">
            <label>
              <input type="checkbox" v-model="formulario.crearCuenta" />
              <strong>Crear cuenta de acceso al sistema para este voluntario</strong>
            </label>
          </div>

          <div v-if="!esEdicion && formulario.crearCuenta" class="vol-campo">
            <label for="password">Contraseña temporal (Mín. 8 caracteres) *</label>
            <input
              id="password"
              v-model="formulario.password"
              type="password"
              placeholder="Ingresa una contraseña"
              :class="{ 'input-invalido': erroresCampo.password }"
              @input="limpiarErrorCampo('password')"
            />
            <span v-if="erroresCampo.password" class="vol-campo-error">{{ erroresCampo.password }}</span>
          </div>

          <div v-if="error" class="vol-error" role="alert">
            <span aria-hidden="true">⚠️</span>
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
import { habilidadService } from '../services/habilidadService'

const router = useRouter()
const route = useRoute()
const esEdicion = ref(!!route.params.id)

const cargando = ref(false)
const error = ref('')
const apellidoInput = ref(null)
const erroresCampo = ref({})

// HU2: Catálogo de habilidades
const habilidadesDisponibles = ref([])

const formulario = ref({
  nombre: '',
  apellido: '',
  documento: '',
  email: '',
  telefono: '',
  crearCuenta: false,
  password: '',
  habilidadesIds: []
})

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const TELEFONO_REGEX = /^[0-9+()\-\s]{6,20}$/

const limpiarErrorCampo = (campo) => {
  if (erroresCampo.value[campo]) {
    delete erroresCampo.value[campo]
  }
  if (error.value) {
    error.value = ''
  }
}

const validar = () => {
  const f = formulario.value
  const nuevosErrores = {}

  if (!(f.nombre ?? '').trim()) nuevosErrores.nombre = 'El nombre es obligatorio'
  if (!(f.apellido ?? '').trim()) nuevosErrores.apellido = 'Se necesita un apellido'
  if (!(f.documento ?? '').trim()) nuevosErrores.documento = 'El documento es obligatorio'

  const email = (f.email ?? '').trim()
  if (!email) {
    nuevosErrores.email = 'El correo es obligatorio'
  } else if (!EMAIL_REGEX.test(email)) {
    nuevosErrores.email = 'El correo no tiene un formato válido'
  }

  const telefono = (f.telefono ?? '').trim()
  if (telefono && !TELEFONO_REGEX.test(telefono)) {
    nuevosErrores.telefono = 'El teléfono no tiene un formato válido'
  }

  if (!esEdicion.value && f.crearCuenta) {
    if (!(f.password ?? '').trim() || f.password.trim().length < 8) {
      nuevosErrores.password = 'La contraseña debe tener al menos 8 caracteres'
    }
  }

  erroresCampo.value = nuevosErrores

  if (nuevosErrores.apellido) {
    apellidoInput.value?.focus()
  }

  const primerError = Object.values(nuevosErrores)[0] || ''
  return primerError
}

onMounted(async () => {
  try {
    habilidadesDisponibles.value = await habilidadService.obtenerHabilidades()
  } catch (err) {
    console.error('No se pudo cargar el catálogo de habilidades:', err)
  }

  if (!esEdicion.value) return

  try {
    const v = await voluntarioService.obtenerVoluntario(route.params.id)
    formulario.value = {
      nombre: v.nombre ?? '',
      apellido: v.apellido ?? '',
      documento: v.documento ?? '',
      email: v.email ?? '',
      telefono: v.telefono ?? '',
      crearCuenta: false,
      password: '',
      habilidadesIds: v.habilidades ? v.habilidades.map(h => h.id) : []
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
      nombre: (formulario.value.nombre ?? '').trim(),
      apellido: (formulario.value.apellido ?? '').trim(),
      documento: (formulario.value.documento ?? '').trim(),
      email: (formulario.value.email ?? '').trim(),
      telefono: (formulario.value.telefono ?? '').trim(),
      habilidadesIds: formulario.value.habilidadesIds
    }

    if (esEdicion.value) {
      await voluntarioService.actualizarVoluntario(route.params.id, payload)
    } else {
      payload.crearCuenta = formulario.value.crearCuenta
      if (payload.crearCuenta) {
        payload.password = formulario.value.password
      }
      await voluntarioService.registrarVoluntario(payload)
    }

    router.push('/voluntarios')
  } catch (err) {
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
  width: min(650px, 100%);
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
.vol-campo input[type="text"],
.vol-campo input[type="email"],
.vol-campo input[type="password"],
.vol-campo input[type="tel"] {
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
  transition: border-color 0.2s, background 0.2s;
}
.vol-campo input:focus {
  border-color: var(--salvia);
}
.vol-campo input.input-invalido {
  border-color: #d96b5c;
  background: #fff7f5;
}
.vol-campo-error {
  color: #a34d41;
  font-size: 12px;
  font-weight: 600;
}

/* HU2: Grid de Habilidades */
.vol-info-habilidades {
  font-size: 12px;
  color: var(--texto-suave);
  padding: 10px;
  background: #f9f9f9;
  border-radius: 10px;
  border: 1px dashed #ccc;
}
.vol-habilidades-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 5px;
}
.vol-habilidad-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: var(--crema-claro);
  border: 1px solid var(--crema);
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.vol-habilidad-item:hover {
  border-color: var(--salvia-claro);
}
.vol-habilidad-item input {
  margin-top: 2px;
  cursor: pointer;
}
.vol-habilidad-texto {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.vol-habilidad-texto strong {
  font-size: 12px;
  color: var(--texto);
}
.vol-habilidad-texto small {
  font-size: 11px;
  color: var(--texto-suave);
  line-height: 1.3;
}

.vol-campo-checkbox {
  display: flex;
  align-items: center;
  background: #fcf8f3;
  padding: 15px;
  border-radius: 12px;
  border: 1px dashed var(--crema);
}
.vol-campo-checkbox label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--texto);
  cursor: pointer;
}
.vol-campo-checkbox input {
  width: 18px;
  height: 18px;
  cursor: pointer;
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
</style>