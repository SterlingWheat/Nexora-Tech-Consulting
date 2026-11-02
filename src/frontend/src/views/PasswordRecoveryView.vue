<template>
  <section class="recuperacion-pagina">
    <form class="recuperacion-tarjeta" @submit.prevent="solicitar">
      <span class="recuperacion-etiqueta">NEXORA TECH CONSULTING</span>
      <h1>Recuperar contraseña</h1>
      <p>Ingresa el correo asociado a tu cuenta y te enviaremos un enlace si está registrado.</p>

      <label for="recovery-email">Correo electrónico</label>
      <input
        id="recovery-email"
        v-model.trim="email"
        type="email"
        name="email"
        autocomplete="email"
        maxlength="150"
        required
        placeholder="nombre@ejemplo.com"
      >

      <p v-if="mensaje" class="recuperacion-mensaje" role="status">{{ mensaje }}</p>
      <p v-if="error" class="recuperacion-error" role="alert">{{ error }}</p>
      <button type="submit" :disabled="cargando">
        {{ cargando ? 'Enviando...' : 'Enviar enlace' }}
      </button>
      <RouterLink class="recuperacion-enlace" to="/login">Volver al inicio de sesión</RouterLink>
    </form>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { passwordResetService } from '../services/passwordResetService'

const email = ref('')
const mensaje = ref('')
const error = ref('')
const cargando = ref(false)

const solicitar = async () => {
  mensaje.value = ''
  error.value = ''
  cargando.value = true
  try {
    const response = await passwordResetService.solicitar(email.value)
    mensaje.value = response.message
  } catch (exception) {
    error.value = exception.message || 'No se pudo enviar la solicitud.'
  } finally {
    cargando.value = false
  }
}
</script>

<style scoped>
.recuperacion-pagina {
  min-height: calc(100vh - 78px);
  display: grid;
  place-items: center;
  padding: 40px 20px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}

.recuperacion-tarjeta {
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

.recuperacion-etiqueta {
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

.recuperacion-tarjeta > p:not(.recuperacion-error, .recuperacion-mensaje) {
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

.recuperacion-error {
  margin: 0;
  color: #a34d41;
  font-size: 13px;
}

.recuperacion-mensaje {
  margin: 0;
  color: var(--verde-oscuro);
  font-size: 13px;
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

.recuperacion-enlace {
  color: var(--verde-oscuro);
  font-size: 13px;
  text-align: center;
}

@media (max-width: 480px) {
  .recuperacion-tarjeta {
    padding: 28px 22px;
  }
}
</style>
