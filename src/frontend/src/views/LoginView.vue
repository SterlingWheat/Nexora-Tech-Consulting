<template>
  <section class="login-pagina">
    <form class="login-tarjeta" @submit.prevent="iniciarSesion">
      <span class="login-etiqueta">CASA AMANDITA</span>
      <h1>Bienvenido</h1>
      <p>Inicia sesión para acceder a la gestión de tareas.</p>

      <label for="email">Correo electrónico</label>
      <input
        id="email"
        v-model.trim="email"
        type="email"
        name="email"
        autocomplete="username"
        required
        placeholder="nombre@ejemplo.com"
      >

      <label for="password">Contraseña</label>
      <input
        id="password"
        v-model="password"
        type="password"
        name="password"
        autocomplete="current-password"
        required
        placeholder="Ingresa tu contraseña"
      >

      <p v-if="error" class="login-error" role="alert">{{ error }}</p>
      <button type="submit" :disabled="cargando">
        {{ cargando ? 'Ingresando...' : 'Iniciar sesión' }}
      </button>
    </form>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const email = ref('')
const password = ref('')
const error = ref(route.query.expired ? 'Tu sesión expiró. Inicia sesión nuevamente.' : '')
const cargando = ref(false)

const iniciarSesion = async () => {
  error.value = ''
  cargando.value = true
  try {
    await authStore.login(email.value, password.value)
    const destino = typeof route.query.redirect === 'string' ? route.query.redirect : '/tareas'
    await router.replace(destino)
  } catch (exception) {
    error.value = exception.message || 'No se pudo iniciar sesión.'
  } finally {
    cargando.value = false
  }
}
</script>

<style scoped>
.login-pagina {
  min-height: calc(100vh - 78px);
  display: grid;
  place-items: center;
  padding: 40px 20px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}

.login-tarjeta {
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

.login-etiqueta {
  color: var(--verde-oscuro);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

h1 {
  margin: 4px 0 0;
  color: var(--texto);
  font-family: "DM Serif Display", serif;
  font-size: 36px;
  font-weight: 400;
}

.login-tarjeta > p:not(.login-error) {
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

.login-error {
  margin: 0;
  color: #a34d41;
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

@media (max-width: 480px) {
  .login-tarjeta {
    padding: 28px 22px;
  }
}
</style>
