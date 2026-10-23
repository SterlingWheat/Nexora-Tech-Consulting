<template>
  <div class="filtros-contenedor">
    <h3>Filtrar Tareas</h3>
    <form @submit.prevent="aplicarFiltros" class="filtros-grid">
      
      <div class="filtro-campo">
        <label for="estado">Estado</label>
        <select id="estado" v-model="filtros.estado">
          <option value="">Todos</option>
          <option value="Pendiente">Pendiente</option>
          <option value="Asignada">Asignada</option>
          <option value="En progreso">En progreso</option>
          <option value="Finalizada">Finalizada</option>
          <option value="Cancelada">Cancelada</option>
        </select>
      </div>

      <div class="filtro-campo">
        <label for="categoria">Categoría</label>
        <select id="categoria" v-model="filtros.idCategoria">
          <option value="">Todas</option>
          <option v-for="cat in categorias" :key="cat.idCategoria" :value="cat.idCategoria">
            {{ cat.nombre }}
          </option>
        </select>
      </div>

      <div class="filtro-campo">
        <label for="responsable">Responsable</label>
        <select id="responsable" v-model="filtros.idResponsable">
          <option value="">Todos</option>
          <option v-for="vol in voluntarios" :key="vol.idVoluntario" :value="vol.idVoluntario">
            {{ vol.nombre }} {{ vol.apellido }}
          </option>
        </select>
      </div>

      <div class="filtro-campo">
        <label for="fechaInicio">Desde (Creación)</label>
        <input id="fechaInicio" type="date" v-model="filtros.fechaInicio" />
      </div>

      <div class="filtro-campo">
        <label for="fechaFin">Hasta (Creación)</label>
        <input id="fechaFin" type="date" v-model="filtros.fechaFin" />
      </div>

      <div class="filtros-acciones">
        <button type="submit" class="btn-aplicar">Aplicar Filtros</button>
        <button type="button" class="btn-limpiar" @click="limpiarFiltros">Limpiar</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { reactive } from 'vue'

const props = defineProps({
  categorias: {
    type: Array,
    default: () => []
  },
  voluntarios: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['filtrar'])

const estadoInicial = {
  estado: '',
  idCategoria: '',
  idResponsable: '',
  fechaInicio: '',
  fechaFin: ''
}

const filtros = reactive({ ...estadoInicial })

const aplicarFiltros = () => {
  const payload = {}
  if (filtros.estado) payload.estado = filtros.estado
  if (filtros.idCategoria) payload.idCategoria = filtros.idCategoria
  if (filtros.idResponsable) payload.idResponsable = filtros.idResponsable
  if (filtros.fechaInicio) payload.fechaInicio = filtros.fechaInicio
  if (filtros.fechaFin) payload.fechaFin = filtros.fechaFin
  
  emit('filtrar', payload)
}

const limpiarFiltros = () => {
  Object.assign(filtros, estadoInicial)
  emit('filtrar', {})
}
</script>

<style scoped>
.filtros-contenedor {
  background: white;
  padding: 20px 24px;
  border-radius: 20px;
  border: 1px solid var(--crema);
  margin-bottom: 24px;
}
.filtros-contenedor h3 {
  margin-top: 0;
  color: var(--texto);
  font-family: "DM Serif Display", serif;
  font-size: 20px;
  font-weight: 400;
}
.filtros-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  align-items: end;
}
.filtro-campo {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.filtro-campo label {
  font-size: 11px;
  font-weight: 700;
  color: var(--texto-suave);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.filtro-campo select,
.filtro-campo input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--crema);
  border-radius: 10px;
  background: var(--crema-claro);
  color: var(--texto);
  font-size: 13px;
  font-family: inherit;
  outline: none;
}
.filtro-campo select:focus,
.filtro-campo input:focus {
  border-color: var(--salvia);
}
.filtros-acciones {
  display: flex;
  gap: 10px;
}
.btn-aplicar {
  padding: 10px 16px;
  background: var(--verde-oscuro);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  flex: 1;
}
.btn-aplicar:hover {
  background: var(--salvia);
}
.btn-limpiar {
  padding: 10px 16px;
  background: transparent;
  color: var(--texto-suave);
  border: 1px solid var(--crema);
  border-radius: 10px;
  font-size: 13px;
  cursor: pointer;
  flex: 1;
}
.btn-limpiar:hover {
  background: var(--crema);
}
</style>