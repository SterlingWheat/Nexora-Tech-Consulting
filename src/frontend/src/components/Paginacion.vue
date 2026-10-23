<template>
  <div class="paginacion-contenedor" v-if="totalPaginas > 1">
    <button 
      class="btn-pagina" 
      @click="cambiar(paginaActual - 1)" 
      :disabled="paginaActual <= 0"
      aria-label="Página anterior"
    >
      Anterior
    </button>
    
    <span class="info-pagina">
      Página <strong>{{ paginaActual + 1 }}</strong> de <strong>{{ totalPaginas }}</strong>
    </span>
    
    <button 
      class="btn-pagina" 
      @click="cambiar(paginaActual + 1)" 
      :disabled="paginaActual >= totalPaginas - 1"
      aria-label="Página siguiente"
    >
      Siguiente
    </button>
  </div>
</template>

<script setup>
const props = defineProps({
  paginaActual: {
    type: Number,
    required: true
  },
  totalPaginas: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['cambiarPagina'])

const cambiar = (nuevaPagina) => {
  if (nuevaPagina >= 0 && nuevaPagina < props.totalPaginas) {
    emit('cambiarPagina', nuevaPagina)
  }
}
</script>

<style scoped>
.paginacion-contenedor {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--crema);
}
.info-pagina {
  font-size: 13px;
  color: var(--texto-suave);
}
.info-pagina strong {
  color: var(--texto);
}
.btn-pagina {
  padding: 8px 16px;
  background: white;
  border: 1px solid var(--crema);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: var(--texto);
  cursor: pointer;
  transition: all 0.2s;
}
.btn-pagina:hover:not(:disabled) {
  background: var(--crema-claro);
  border-color: var(--salvia-claro);
}
.btn-pagina:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>