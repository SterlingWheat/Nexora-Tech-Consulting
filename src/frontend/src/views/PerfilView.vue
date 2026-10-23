<template>
  <section class="perfil-pagina">
    <div class="perfil-tarjeta">
      <RouterLink v-if="esPerfilDeOtro" to="/usuarios" class="perfil-volver">
        ← Volver a voluntarios
      </RouterLink>
      <span class="perfil-etiqueta">PERFIL DE VOLUNTARIO</span>
      <h1>{{ usuario?.nombre || 'Mi perfil' }}</h1>

      <p v-if="cargando" class="perfil-estado">Cargando perfil...</p>
      <p v-else-if="error" class="perfil-error" role="alert">{{ error }}</p>
      <dl v-else-if="usuario" class="perfil-datos">
        <div><dt>Nombre</dt><dd>{{ usuario.nombre }}</dd></div>
        <div><dt>Correo electrónico</dt><dd>{{ usuario.email }}</dd></div>
        <div><dt>Rol</dt><dd>{{ rolLegible(usuario.rol) }}</dd></div>
        <div><dt>Estado</dt><dd>{{ usuario.activo ? 'Activo' : 'Inactivo' }}</dd></div>
        <div><dt>Fecha de registro</dt><dd>{{ fechaLegible(usuario.fechaCreacion) }}</dd></div>
      </dl>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { perfilService } from '../services/perfilService'

const route = useRoute()
const usuario = ref(null)
const cargando = ref(true)
const error = ref('')
const esPerfilDeOtro = computed(() => Boolean(route.params.id))

const cargarPerfil = async () => {
  cargando.value = true
  error.value = ''
  try {
    usuario.value = esPerfilDeOtro.value
      ? await perfilService.obtenerPerfil(route.params.id)
      : await perfilService.obtenerMiPerfil()
  } catch (exception) {
    error.value = exception.message || 'No se pudo cargar el perfil.'
  } finally {
    cargando.value = false
  }
}

const rolLegible = (rol) => ({
  ADMINISTRADOR: 'Administrador',
  TI: 'TI',
  GESTOR_TAREAS: 'Gestor de Tareas',
  VOLUNTARIO: 'Voluntario'
}[rol] || rol)

const fechaLegible = (fecha) => {
  if (!fecha) return 'No disponible'
  const date = new Date(fecha)
  return Number.isNaN(date.getTime()) ? 'No disponible' : date.toLocaleDateString()
}

onMounted(cargarPerfil)
watch(() => route.params.id, cargarPerfil)
</script>

<style scoped>
.perfil-pagina {
  min-height: calc(100vh - 78px);
  display: grid;
  place-items: start center;
  padding: 70px 20px;
  background: radial-gradient(ellipse at 100% 0%, rgba(241, 205, 183, 0.25), transparent 42%), var(--crema-claro);
}

.perfil-tarjeta {
  width: min(100%, 760px);
  box-sizing: border-box;
  padding: 38px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 23px;
  box-shadow: 0 15px 50px rgba(81, 72, 62, 0.06);
}

.perfil-volver {
  display: inline-block;
  margin-bottom: 26px;
  color: var(--verde-oscuro);
  font-size: 13px;
  text-decoration: none;
}

.perfil-etiqueta {
  display: block;
  color: var(--verde-oscuro);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

h1 {
  margin: 12px 0 28px;
  color: var(--texto);
  font-family: "DM Serif Display", serif;
  font-size: 36px;
  font-weight: 400;
}

.perfil-datos {
  margin: 0;
}

.perfil-datos div {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) 2fr;
  gap: 20px;
  padding: 17px 0;
  border-top: 1px solid var(--crema);
}

dt {
  color: var(--texto-suave);
  font-size: 13px;
}

dd {
  margin: 0;
  color: var(--texto);
  font-size: 14px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.perfil-error {
  color: #a34d41;
}

.perfil-estado {
  color: var(--texto-suave);
}

@media (max-width: 520px) {
  .perfil-tarjeta {
    padding: 26px 20px;
  }

  .perfil-datos div {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}
</style>
