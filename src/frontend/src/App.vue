<template>
  <div class="app">
    <!-- Cabecera simple integrada con la paleta de colores del proyecto -->
    <header class="app-cabecera">
      <div class="cabecera-contenedor">
        <RouterLink to="/tareas" class="logo-link">
          <strong>Casa Amandita</strong>
          <small>Tareas Operativas</small>
        </RouterLink>
        <nav v-if="authStore.isAuthenticated" class="nav-links">
          <RouterLink to="/tareas" class="nav-btn">Listado de Tareas</RouterLink>
          
          <RouterLink to="/perfil" class="nav-btn">Mi perfil</RouterLink>
          
          <RouterLink to="/turnos" class="nav-btn">Turnos</RouterLink>
          
          <RouterLink v-if="authStore.rol === 'ADMINISTRADOR'" to="/usuarios" class="nav-btn">
            Voluntarios
          </RouterLink>
          
          <RouterLink
            v-if="puedeGestionarTareas"
            to="/tareas/nueva"
            class="nav-btn-nuevo"
          >
            + Nueva Tarea
          </RouterLink>
          
          <button class="nav-btn-cerrar" type="button" @click="cerrarSesion">Cerrar sesión</button>
        </nav>
      </div>
    </header>

    <!-- Aquí Vue Router renderiza automáticamente TareasView o TareaFormView -->
    <main class="app-contenido">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/authStore'

const authStore = useAuthStore()
const router = useRouter()
const puedeGestionarTareas = computed(() =>
  ['ADMINISTRADOR', 'GESTOR_TAREAS'].includes(authStore.rol)
)

const cerrarSesion = async () => {
  authStore.logout()
  await router.replace('/login')
}

const manejarSesionExpirada = () => {
  authStore.logout()
  router.replace({ name: 'login', query: { expired: '1' } })
}

onMounted(() => window.addEventListener('auth:expired', manejarSesionExpirada))
onUnmounted(() => window.removeEventListener('auth:expired', manejarSesionExpirada))
</script>

<style>
/* Estilos globales y layout base */
:root {
  --salvia: #A5AA94;
  --salvia-claro: #C4C9B2;
  --crema: #EFE3D3;
  --durazno: #F1CDB7;
  --coral: #EF9D87;
  --crema-claro: #FCF8F3;
  --texto: #51483E;
  --texto-suave: #7B766D;
  --verde-oscuro: #737D61;
}

body {
  margin: 0;
  font-family: "DM Sans", -apple-system, BlinkMacSystemFont, sans-serif;
  background-color: var(--crema-claro);
  color: var(--texto);
}

.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-cabecera {
  background: white;
  border-bottom: 1px solid var(--crema);
  padding: 16px 24px;
}

.cabecera-contenedor {
  width: min(1150px, 100%);
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo-link {
  text-decoration: none;
  color: var(--texto);
  display: flex;
  flex-direction: column;
}

.logo-link strong {
  font-size: 18px;
  color: var(--verde-oscuro);
}

.logo-link small {
  font-size: 11px;
  color: var(--texto-suave);
  letter-spacing: 1px;
}

.nav-links {
  display: flex;
  gap: 12px;
  align-items: center;
}

.nav-btn {
  text-decoration: none;
  color: var(--texto);
  font-size: 13px;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 20px;
  transition: background 0.2s;
}

.nav-btn:hover {
  background: var(--crema);
}

.nav-btn-nuevo {
  text-decoration: none;
  background: var(--verde-oscuro);
  color: white;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 18px;
  border-radius: 20px;
  transition: opacity 0.2s;
}

.nav-btn-nuevo:hover {
  opacity: 0.9;
}

.nav-btn-cerrar {
  border: 0;
  background: transparent;
  color: var(--texto);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.app-contenido {
  flex: 1;
}
</style>