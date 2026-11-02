<template>
  <section class="restablecer-pagina">
    <form v-if="tokenValido && !restablecido" class="restablecer-tarjeta" @submit.prevent="restablecer">
      <span class="restablecer-etiqueta">NEXORA TECH CONSULTING</span>
      <h1>Nueva contraseña</h1>
      <p>La contraseña debe tener al menos 8 caracteres.</p>

      <label for="new-password">Nueva contraseña</label>
      <input
        id="new-password"
        v-model="newPassword"
        type="password"
        name="newPassword"
        autocomplete="new-password"
        minlength="8"
        maxlength="72"
        required
      >

      <label for="confirm-password">Confirmar contraseña</label>
      <input
        id="confirm-password"
        v-model="confirmPassword"
        type="password"
        name="confirmPassword"
        autocomplete="new-password"
        minlength="8"
        maxlength="72"
        required
      >

      <p v-if="error" class="restablecer-error" role="alert">{{ error }}</p>
      <button type="submit" :disabled="cargando">
        {{ cargando ? 'Guardando...' : 'Restablecer contraseña' }}
      </button>
    </form>

    <div v-else class="restablecer-tarjeta" aria-live="polite">
      <span class="restablecer-etiqueta">NEXORA TECH CONSULTING</span>
      <h1>{{ restablecido ? 'Contraseña actualizada' : 'Enlace no válido' }}</h1>
      <p>{{ mensaje }}</p>
      <RouterLink class="restablecer-enlace" to="/login">Volver al inicio de sesión</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { passwordResetService } from '../services/passwordResetService'

const token = new URLSearchParams(window.location.hash.slice(1)).get('token') || ''
const tokenValido = ref(false)
const restablecido = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const mensaje = ref('Estamos comprobando el enlace de recuperación.')
const cargando = ref(false)

onMounted(async () => {
  if (!token) {
    mensaje.value = 'Solicita un nuevo enlace de recuperación para continuar.'
    return
  }

  try {
    await passwordResetService.validar(token)
    tokenValido.value = true
    mensaje.value = ''
  } catch {
    mensaje.value = 'El enlace no es válido o expiró. Solicita uno nuevo para continuar.'
  }
})

const restablecer = async () => {
  error.value = ''
  if (newPassword.value !== confirmPassword.value) {
    error.value = 'Las contraseñas no coinciden.'
    return
  }
  if (newPassword.value.length < 8) {
    error.value = 'La contraseña debe tener al menos 8 caracteres.'
    return
  }

  cargando.value = true
  try {
    const response = await passwordResetService.restablecer(
      token,
      newPassword.value,
      confirmPassword.value
    )
    mensaje.value = response.message
    restablecido.value = true
    tokenValido.value = false
  } catch (exception) {
    error.value = exception.message || 'No se pudo restablecer la contraseña.'
  } finally {
    cargando.value = false
  }
}
</script>

<style scoped>
.restablecer-pagina {
  min-height: calc(100vh - 78px);
  display: grid;
  place-items: center;
  padding: 40px 20px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}

.restablecer-tarjeta {
  width: min(100%, 420px);
  display: flex;
  flex-direction: column;
  gap: 13px;
  padding: 38px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 23px;
  box-shadow: 0 15px 50px rgba(81, 72, 62, 0.08);
}

.restablecer-etiqueta {
  color: var(--verde-oscuro);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

h1 {
  margin: 4px 0 0;
  color: var(--texto);
  font-family: "DM Serif Display", serif;
  font-size: 34px;
  font-weight: 400;
}

.restablecer-tarjeta > p {
  margin: 0 0 12px;
  color: var(--texto-suave);
  font-size: 14px;
}

label {
  color: var(--texto);
  font-size: 13px;
  font-weight: 600;
}

input {
  width: 100%;
  box-sizing: border-box;
  padding: 13px 14px;
  border: 1px solid var(--salvia-claro);
  border-radius: 10px;
  color: var(--texto);
  font: inherit;
}

input:focus {
  outline: 2px solid var(--verde-oscuro);
  outline-offset: 1px;
}

.restablecer-error {
  color: #a34d41 !important;
}

button {
  margin-top: 8px;
  padding: 13px;
  border: 0;
  border-radius: 30px;
  background: var(--verde-oscuro);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

button:disabled {
  cursor: wait;
  opacity: 0.7;
}

.restablecer-enlace {
  color: var(--verde-oscuro);
  font-size: 13px;
  text-align: center;
}

@media (max-width: 480px) {
  .restablecer-tarjeta {
    padding: 28px 22px;
  }
}
</style>
