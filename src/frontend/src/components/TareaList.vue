<template>
  <div class="tareas-tabla-contenedor">
    <table class="tareas-tabla">
      <thead>
        <tr>
          <th>Título</th>
          <th>Categoría</th>
          <th>Estado</th>
          <th>Avance</th>
          <th>Acciones</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="tarea in tareas" :key="tarea.idTarea">
          <td>
            <div class="tareas-datos">
              <strong>{{ tarea.titulo }}</strong>
              <small>{{ tarea.descripcion }}</small>
            </div>
          </td>
          <td>
            <span class="tareas-categoria">{{ tarea.nombreCategoria }}</span>
          </td>
          <td>
            <span :class="['tareas-badge', `badge-${tarea.estado.toLowerCase().replace(' ', '-')}`]">
              {{ tarea.estado }}
            </span>
          </td>
          <td>
            <strong>{{ tarea.avance }}%</strong>
          </td>
          <td>
            <button 
              class="tareas-boton-editar" 
              :disabled="tarea.estado === 'Finalizada' || tarea.estado === 'Cancelada'"
              @click="$emit('editar', tarea.idTarea)"
              :title="tarea.estado === 'Finalizada' || tarea.estado === 'Cancelada' ? 'No se puede editar una tarea Finalizada o Cancelada' : 'Editar tarea'"
            >
              Editar
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  tareas: {
    type: Array,
    required: true,
    default: () => []
  }
})

defineEmits(['editar'])
</script>

<style scoped>
.tareas-tabla-contenedor {
  overflow-x: auto; /*[cite: 3] */
}

.tareas-tabla {
  width: 100%; /*[cite: 3] */
  border-collapse: collapse; /*[cite: 3] */
  text-align: left; /*[cite: 3] */
}

.tareas-tabla th {
  padding: 16px; /*[cite: 3] */
  background: var(--crema-claro); /*[cite: 3] */
  color: var(--texto-suave); /*[cite: 3] */
  font-size: 11px; /*[cite: 3] */
  font-weight: 700; /*[cite: 3] */
  white-space: nowrap; /*[cite: 3] */
}

.tareas-tabla th:first-child { border-radius: 10px 0 0 10px; } /*[cite: 3] */
.tareas-tabla th:last-child { border-radius: 0 10px 10px 0; } /*[cite: 3] */

.tareas-tabla td {
  padding: 22px 16px; /*[cite: 3] */
  border-bottom: 1px solid var(--crema); /*[cite: 3] */
  vertical-align: middle; /*[cite: 3] */
}

.tareas-tabla tbody tr:last-child td {
  border-bottom: none; /*[cite: 3] */
}

.tareas-datos {
  display: flex; /*[cite: 3] */
  flex-direction: column; /*[cite: 3] */
  gap: 5px; /*[cite: 3] */
}

.tareas-datos strong {
  color: var(--texto); /*[cite: 3] */
  font-size: 13px; /*[cite: 3] */
}

.tareas-datos small {
  color: var(--texto-suave); /*[cite: 3] */
  font-size: 12px; /*[cite: 3] */
}

.tareas-categoria {
  font-size: 12px;
  color: var(--texto);
}

.tareas-badge {
  display: inline-flex; /*[cite: 3] */
  padding: 8px 13px; /*[cite: 3] */
  border-radius: 30px; /*[cite: 3] */
  font-size: 11px; /*[cite: 3] */
  font-weight: 700; /*[cite: 3] */
  white-space: nowrap; /*[cite: 3] */
}

.badge-pendiente { background: #F8E4D4; color: #A46B4C; } /*[cite: 3] */
.badge-en-progreso { background: #E3E9D7; color: #596B43; } /*[cite: 3] */
.badge-finalizada { background: #E8F2E5; color: #50704B; } /*[cite: 3] */
.badge-cancelada { background: #FFF0EB; color: #A34D41; } /*[cite: 3] */

.tareas-boton-editar {
  padding: 8px 16px;
  border: 1px solid var(--salvia-claro); /*[cite: 3] */
  border-radius: 10px; /*[cite: 3] */
  background: transparent;
  color: var(--texto);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.tareas-boton-editar:hover:not(:disabled) {
  background: var(--crema); /*[cite: 3] */
}

.tareas-boton-editar:disabled {
  opacity: 0.4; /*[cite: 3] */
  cursor: not-allowed; /*[cite: 3] */
}
</style>