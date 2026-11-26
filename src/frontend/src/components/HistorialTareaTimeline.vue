<template>
  <div class="historial-timeline">
    
    <div v-if="loading" class="timeline-loading" data-test="loading">
      Cargando historial...
    </div>
    
    <div v-else-if="error" class="timeline-error" data-test="error">
      {{ error }}
    </div>
    
    <div v-else-if="historial.length === 0" class="timeline-empty" data-test="empty">
      No hay registros en el historial para esta tarea.
    </div>
    
    <ol v-else class="timeline-list" data-test="timeline">
      <li v-for="registro in historial" :key="registro.idHistorial" class="timeline-item" data-test="timeline-item">
        <span class="timeline-icon-container" :class="obtenerClasePorTipo(registro.tipoCambio)">
          <svg class="timeline-icon" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="obtenerIconoPorTipo(registro.tipoCambio)"/>
          </svg>
        </span>
        <div class="timeline-content">
          <div class="timeline-header">
            <h4>{{ formatearTipo(registro.tipoCambio) }}</h4>
            <time>{{ formatearFecha(registro.fechaRegistro) }}</time>
          </div>
          <p class="timeline-desc">{{ registro.detalle }}</p>
          <p class="timeline-author">Por: {{ registro.nombreUsuario || 'Sistema' }}</p>
        </div>
      </li>
    </ol>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { historialTareaService } from '../services/historialTareaService';

const props = defineProps({
  tareaId: {
    type: [Number, String],
    required: true
  }
});

const historial = ref([]);
const loading = ref(false);
const error = ref('');

const cargarHistorial = async () => {
  if (!props.tareaId) return;
  
  loading.value = true;
  error.value = '';
  
  try {
    const data = await historialTareaService.obtenerHistorial(props.tareaId);
    historial.value = data || [];
  } catch (err) {
    error.value = err.message || 'Error al cargar el historial de la tarea.';
  } finally {
    loading.value = false;
  }
};

const formatearFecha = (fechaISO) => {
  if (!fechaISO) return '';
  return new Date(fechaISO).toLocaleString('es-BO', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
};

const formatearTipo = (tipo) => {
  const tipos = {
    'CREACION': 'Creación',
    'ESTADO': 'Cambio de Estado',
    'ASIGNACION': 'Asignación Modificada',
    'FECHA': 'Fecha Reprogramada',
    'EDICION': 'Edición General'
  };
  return tipos[tipo] || tipo;
};

const obtenerClasePorTipo = (tipo) => {
  const clases = {
    'CREACION': 'bg-green',
    'ESTADO': 'bg-blue',
    'ASIGNACION': 'bg-purple',
    'FECHA': 'bg-orange',
    'EDICION': 'bg-gray'
  };
  return clases[tipo] || 'bg-gray';
};

const obtenerIconoPorTipo = (tipo) => {
  switch (tipo) {
    case 'CREACION': return 'M12 4v16m8-8H4'; 
    case 'ESTADO': return 'M9 5l7 7-7 7'; 
    case 'ASIGNACION': return 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z'; 
    case 'FECHA': return 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'; 
    default: return 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z'; 
  }
};

watch(() => props.tareaId, cargarHistorial);

onMounted(() => {
  cargarHistorial();
});
</script>

<style scoped>
.historial-timeline {
  width: 100%;
  margin-top: 15px;
  font-family: inherit;
}

.timeline-loading, .timeline-empty {
  font-size: 14px;
  color: var(--texto-suave, #6B7280);
  padding: 15px 0;
  font-style: italic;
}

.timeline-error {
  font-size: 14px;
  color: #A34D41;
  background-color: #FFF0EB;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #E9B0A5;
}

/* Estilos nativos de la línea de tiempo */
.timeline-list {
  position: relative;
  border-left: 2px solid #E5E7EB;
  margin-left: 12px;
  padding-left: 0;
  list-style: none;
}

.timeline-item {
  position: relative;
  margin-bottom: 24px;
  margin-left: 24px;
}

.timeline-icon-container {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  left: -39px;
  top: 0;
  border: 4px solid white;
}

/* Asegurar que el SVG no se desborde */
.timeline-icon {
  width: 14px;
  height: 14px;
  color: white;
}

.bg-green { background-color: #10B981; }
.bg-blue { background-color: #3B82F6; }
.bg-purple { background-color: #8B5CF6; }
.bg-orange { background-color: #F59E0B; }
.bg-gray { background-color: #6B7280; }

.timeline-content {
  background-color: #F9FAFB;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #F3F4F6;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 6px;
}

.timeline-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.timeline-header time {
  font-size: 12px;
  font-weight: 400;
  color: #9CA3AF;
}

.timeline-desc {
  margin: 0 0 6px 0;
  font-size: 13px;
  color: #4B5563;
  line-height: 1.5;
}

.timeline-author {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--verde-oscuro, #4B6E91);
}
</style>