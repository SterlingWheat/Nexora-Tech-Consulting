<template>
  <div class="app">
    <header class="app-cabecera">
      <div class="cabecera-contenedor">
        <RouterLink to="/tareas" class="logo-link">
          <strong>Casa Amandita</strong>
          <small>Tareas Operativas</small>
        </RouterLink>

        <nav v-if="authStore.isAuthenticated" class="nav-links" aria-label="Navegación principal">
          <RouterLink v-if="authStore.rol === 'ADMINISTRADOR'" to="/dashboard" class="nav-btn">
            Panel
          </RouterLink>

          <RouterLink to="/tareas" class="nav-btn">Listado de Tareas</RouterLink>
          <RouterLink to="/turnos" class="nav-btn">Turnos</RouterLink>

          <RouterLink v-if="puedeVerCalendario" to="/calendario-disponibilidad" class="nav-btn">
            Calendario
          </RouterLink>

          <RouterLink v-if="puedeGestionarTareas" to="/turnos/calendario" class="nav-btn">
            Calendario de turnos
          </RouterLink>

          <!-- Enlace correcto para la gestión del padrón de voluntarios -->
          <RouterLink to="/voluntarios" class="nav-btn">Voluntarios</RouterLink>

          <RouterLink
            v-if="authStore.rol === 'ADMINISTRADOR'"
            to="/habilidades"
            class="nav-btn"
          >
            Habilidades
          </RouterLink>

          <RouterLink
            v-if="authStore.rol === 'ADMINISTRADOR'"
            to="/usuarios"
            class="nav-btn"
          >
            Usuarios
          </RouterLink>

          <RouterLink
            v-if="authStore.rol === 'ADMINISTRADOR'"
            to="/plantillas-notificacion"
            class="nav-btn"
          >
            Plantillas de correo
          </RouterLink>

          <RouterLink
            v-if="authStore.rol === 'ADMINISTRADOR'"
            to="/reportes"
            class="nav-btn"
          >
            Reportes
          </RouterLink>

          <RouterLink to="/perfil" class="nav-btn">Mi perfil</RouterLink>

          <div class="nav-acciones">
            <RouterLink
              v-if="puedeGestionarTareas"
              to="/tareas/nueva"
              class="nav-btn-nuevo"
            >
              + Nueva Tarea
            </RouterLink>

            <button class="nav-btn-cerrar" type="button" @click="cerrarSesion">
              Cerrar sesión
            </button>
          </div>
        </nav>
      </div>
    </header>

    <main class="app-contenido">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from './stores/authStore'

const authStore = useAuthStore()
const router = useRouter()

const puedeGestionarTareas = computed(() =>
  ['ADMINISTRADOR', 'GESTOR_TAREAS'].includes(authStore.rol)
)

const puedeVerCalendario = computed(() =>
  ['ADMINISTRADOR', 'TI', 'GESTOR_TAREAS'].includes(authStore.rol)
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
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--crema);
  padding: 12px 24px;
}

.cabecera-contenedor {
  width: min(1360px, 100%);
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.logo-link {
  text-decoration: none;
  color: var(--texto);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  white-space: nowrap;
}

.logo-link strong {
  font-size: 18px;
  line-height: 1.2;
  color: var(--verde-oscuro);
}

.logo-link small {
  font-size: 11px;
  color: var(--texto-suave);
  letter-spacing: 1px;
}

.nav-links {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  flex-wrap: nowrap;
}

.nav-btn {
  text-decoration: none;
  color: var(--texto);
  font-size: 13px;
  font-weight: 600;
  padding: 8px 12px;
  border-radius: 20px;
  white-space: nowrap;
  transition: background 0.2s, color 0.2s;
}

.nav-btn:hover {
  background: var(--crema);
}

.nav-btn.router-link-active {
  background: var(--crema-claro);
  color: var(--verde-oscuro);
}

.nav-acciones {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
  padding-left: 12px;
  border-left: 1px solid var(--crema);
}

.nav-btn-nuevo {
  text-decoration: none;
  background: var(--verde-oscuro);
  color: white;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 20px;
  white-space: nowrap;
  transition: opacity 0.2s, transform 0.15s;
}

.nav-btn-nuevo:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.nav-btn-cerrar {
  border: 1px solid var(--crema);
  background: var(--crema-claro);
  color: var(--texto);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  padding: 7px 14px;
  border-radius: 20px;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
}

.nav-btn-cerrar:hover {
  background: #fff0eb;
  border-color: #e9b0a5;
  color: #a34d41;
}

@media (max-width: 1120px) {
  .nav-links {
    flex-wrap: wrap;
  }

  .nav-acciones {
    margin-left: 0;
    padding-left: 0;
    border-left: 0;
  }
}

.app-contenido {
  flex: 1;
}
</style>