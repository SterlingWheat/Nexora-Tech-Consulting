<template>
  <section class="usuarios-pagina">
    <div class="usuarios-contenedor">
      <span class="usuarios-etiqueta">ADMINISTRACIÓN</span>
      <h1>Usuarios del sistema</h1>
      <p>Crea y administra las cuentas, roles y estados de acceso.</p>

      <button v-if="!formularioVisible" class="usuarios-boton" @click="iniciarCreacion">
        Crear usuario
      </button>

      <form v-if="formularioVisible" class="usuarios-formulario" @submit.prevent="guardarUsuario">
        <h2>{{ usuarioEditando ? 'Editar usuario' : 'Crear usuario' }}</h2>
        <label>
          Nombre
          <input v-model="formulario.nombre" required maxlength="100" autocomplete="name">
        </label>
        <label>
          Correo electrónico
          <input v-model="formulario.email" type="email" required maxlength="150" autocomplete="email">
        </label>
        <label>
          {{ usuarioEditando ? 'Nueva contraseña (opcional)' : 'Contraseña' }}
          <input
            v-model="formulario.password"
            type="password"
            :required="!usuarioEditando"
            minlength="8"
            autocomplete="new-password"
          >
        </label>
        <label>
          Rol
          <select v-model="formulario.rol" required>
            <option disabled value="">Selecciona un rol</option>
            <option v-for="rol in roles" :key="rol.valor" :value="rol.valor">
              {{ rol.etiqueta }}
            </option>
          </select>
        </label>
        <p v-if="error" class="usuarios-error" role="alert">{{ error }}</p>
        <div class="usuarios-acciones-formulario">
          <button class="usuarios-boton" type="submit" :disabled="guardando">
            {{ guardando ? 'Guardando...' : 'Guardar' }}
          </button>
          <button class="usuarios-boton secundario" type="button" @click="cancelarFormulario">
            Cancelar
          </button>
        </div>
      </form>

      <div class="usuarios-filtros" aria-label="Filtros de usuarios">
        <label>
          Filtrar por rol
          <select v-model="filtros.rol" @change="cargarUsuarios">
            <option value="">Todos los roles</option>
            <option v-for="rol in roles" :key="rol.valor" :value="rol.valor">
              {{ rol.etiqueta }}
            </option>
          </select>
        </label>
        <label>
          Filtrar por estado
          <select v-model="filtros.activo" @change="cargarUsuarios">
            <option value="">Todos los estados</option>
            <option value="true">Activo</option>
            <option value="false">Inactivo</option>
          </select>
        </label>
      </div>

      <p v-if="cargando" class="usuarios-estado">Cargando usuarios...</p>
      <p v-else-if="error && !formularioVisible" class="usuarios-error" role="alert">{{ error }}</p>
      <div v-else-if="usuarios.length" class="usuarios-tabla-contenedor">
        <table>
          <thead>
            <tr><th>Nombre</th><th>Correo electrónico</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr>
          </thead>
          <tbody>
            <tr v-for="usuario in usuarios" :key="usuario.idUsuario">
              <td>{{ usuario.nombre }}</td>
              <td>{{ usuario.email }}</td>
              <td>{{ rolLegible(usuario.rol) }}</td>
              <td>{{ usuario.activo ? 'Activo' : 'Inactivo' }}</td>
              <td class="usuarios-acciones">
                <RouterLink :to="`/usuarios/${usuario.idUsuario}/perfil`">Ver perfil</RouterLink>
                <button class="usuarios-enlace" @click="iniciarEdicion(usuario)">Editar</button>
                <button
                  v-if="usuario.activo"
                  class="usuarios-enlace peligro"
                  @click="desactivarUsuario(usuario)"
                >
                  Desactivar
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else-if="!cargando" class="usuarios-estado">No hay usuarios que coincidan con los filtros.</p>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { usuarioService } from '../services/usuarioService'

const roles = [
  { valor: 'ADMINISTRADOR', etiqueta: 'Administrador' },
  { valor: 'TI', etiqueta: 'TI' },
  { valor: 'GESTOR_TAREAS', etiqueta: 'Gestor de Tareas' },
  { valor: 'VOLUNTARIO', etiqueta: 'Voluntario' }
]

const usuarios = ref([])
const cargando = ref(true)
const guardando = ref(false)
const error = ref('')
const formularioVisible = ref(false)
const usuarioEditando = ref(null)
const filtros = reactive({ rol: '', activo: '' })
const formulario = reactive({ nombre: '', email: '', password: '', rol: '' })

const rolLegible = (rol) => roles.find((opcion) => opcion.valor === rol)?.etiqueta || rol

const cargarUsuarios = async () => {
  cargando.value = true
  error.value = ''
  try {
    usuarios.value = await usuarioService.listar(filtros)
  } catch (exception) {
    error.value = exception.message || 'No se pudo cargar la lista de usuarios.'
  } finally {
    cargando.value = false
  }
}

const limpiarFormulario = () => {
  formulario.nombre = ''
  formulario.email = ''
  formulario.password = ''
  formulario.rol = ''
}

const iniciarCreacion = () => {
  limpiarFormulario()
  usuarioEditando.value = null
  formularioVisible.value = true
  error.value = ''
}

const iniciarEdicion = (usuario) => {
  limpiarFormulario()
  usuarioEditando.value = usuario
  formulario.nombre = usuario.nombre
  formulario.email = usuario.email
  formulario.rol = usuario.rol
  formularioVisible.value = true
  error.value = ''
}

const cancelarFormulario = () => {
  formularioVisible.value = false
  usuarioEditando.value = null
  limpiarFormulario()
  error.value = ''
}

const guardarUsuario = async () => {
  guardando.value = true
  error.value = ''
  const datos = {
    nombre: formulario.nombre,
    email: formulario.email,
    rol: formulario.rol
  }
  if (!usuarioEditando.value || formulario.password) {
    datos.password = formulario.password
  }

  try {
    if (usuarioEditando.value) {
      await usuarioService.actualizar(usuarioEditando.value.idUsuario, datos)
    } else {
      await usuarioService.crear(datos)
    }
    cancelarFormulario()
    await cargarUsuarios()
  } catch (exception) {
    error.value = exception.message || 'No se pudo guardar el usuario.'
  } finally {
    guardando.value = false
  }
}

const desactivarUsuario = async (usuario) => {
  if (!window.confirm(`¿Desactivar la cuenta de ${usuario.nombre}?`)) return
  error.value = ''
  try {
    await usuarioService.desactivar(usuario.idUsuario)
    await cargarUsuarios()
  } catch (exception) {
    error.value = exception.message || 'No se pudo desactivar el usuario.'
  }
}

onMounted(cargarUsuarios)
</script>

<style scoped>
.usuarios-pagina {
  min-height: calc(100vh - 78px);
  padding: 70px 24px;
  background: var(--crema-claro);
}

.usuarios-contenedor {
  width: min(1100px, 100%);
  margin: auto;
}

.usuarios-etiqueta {
  color: var(--verde-oscuro);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

h1 {
  margin: 14px 0 8px;
  color: var(--texto);
  font-family: "DM Serif Display", serif;
  font-size: 40px;
  font-weight: 400;
}

h2 {
  margin: 0;
  color: var(--texto);
}

p {
  color: var(--texto-suave);
}

.usuarios-boton {
  padding: 11px 17px;
  border: 0;
  border-radius: 8px;
  background: var(--verde-oscuro);
  color: white;
  cursor: pointer;
  font: inherit;
  font-weight: 600;
}

.usuarios-boton:disabled {
  cursor: wait;
  opacity: 0.7;
}

.usuarios-boton.secundario {
  border: 1px solid var(--verde-oscuro);
  background: white;
  color: var(--verde-oscuro);
}

.usuarios-formulario,
.usuarios-filtros {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: end;
  margin-top: 24px;
}

.usuarios-formulario {
  padding: 22px;
  border: 1px solid var(--crema);
  border-radius: 12px;
  background: white;
}

.usuarios-formulario h2,
.usuarios-formulario .usuarios-error,
.usuarios-acciones-formulario {
  flex-basis: 100%;
}

.usuarios-formulario label,
.usuarios-filtros label {
  display: grid;
  flex: 1 1 190px;
  gap: 7px;
  color: var(--texto);
  font-size: 13px;
  font-weight: 600;
}

input,
select {
  min-height: 40px;
  padding: 8px 10px;
  border: 1px solid #d9d1c6;
  border-radius: 7px;
  background: white;
  color: var(--texto);
  font: inherit;
}

.usuarios-acciones-formulario {
  display: flex;
  gap: 10px;
}

.usuarios-tabla-contenedor {
  margin-top: 24px;
  overflow-x: auto;
  border: 1px solid var(--crema);
  border-radius: 16px;
  background: white;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

th,
td {
  padding: 16px;
  border-bottom: 1px solid var(--crema);
  color: var(--texto);
  font-size: 13px;
  white-space: nowrap;
}

th {
  color: var(--texto-suave);
  background: #fcf8f3;
}

.usuarios-acciones {
  display: flex;
  gap: 12px;
  align-items: center;
}

a,
.usuarios-enlace {
  padding: 0;
  border: 0;
  background: none;
  color: var(--verde-oscuro);
  cursor: pointer;
  font: inherit;
  font-weight: 600;
  text-decoration: underline;
}

.usuarios-enlace.peligro {
  color: #a34d41;
}

.usuarios-error {
  color: #a34d41;
}

.usuarios-estado {
  padding: 30px 0;
}
</style>
