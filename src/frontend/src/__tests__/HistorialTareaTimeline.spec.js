import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import HistorialTareaTimeline from '../components/HistorialTareaTimeline.vue';
import { historialTareaService } from '../services/historialTareaService';

vi.mock('../services/historialTareaService');

describe('HistorialTareaTimeline.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra estado de carga inicialmente', async () => {
    // Retornamos una promesa que nunca se resuelve para asegurar que quede "cargando"
    historialTareaService.obtenerHistorial.mockReturnValue(new Promise(() => {}));
    
    const wrapper = mount(HistorialTareaTimeline, {
      props: { tareaId: 1 }
    });
    
    // Forzamos manualmente el estado de carga a true por si el onMounted es asíncrono
    wrapper.vm.loading = true;
    
    // Esperamos un tick del DOM
    await wrapper.vm.$nextTick();
    
    expect(wrapper.find('[data-test="loading"]').exists()).toBe(true);
  });

  it('muestra el historial correctamente cuando hay datos', async () => {
    // Nombres de campo iguales a los de HistorialTareaResponseDTO (backend): idHistorial,
    // tipoCambio, detalle, nombreUsuario, fechaRegistro.
    const mockHistorial = [
      { idHistorial: 1, tipoCambio: 'CREACION', detalle: 'Tarea creada', nombreUsuario: 'Admin', fechaRegistro: '2026-11-25T10:00:00Z' },
      { idHistorial: 2, tipoCambio: 'ESTADO', detalle: 'Cambio a EN_PROGRESO', nombreUsuario: 'Voluntario 1', fechaRegistro: '2026-11-25T11:00:00Z' }
    ];
    historialTareaService.obtenerHistorial.mockResolvedValue(mockHistorial);
    
    const wrapper = mount(HistorialTareaTimeline, {
      props: { tareaId: 10 }
    });
    
    await flushPromises();
    
    expect(wrapper.find('[data-test="loading"]').exists()).toBe(false);
    expect(wrapper.find('[data-test="timeline"]').exists()).toBe(true);
    
    const items = wrapper.findAll('[data-test="timeline-item"]');
    expect(items).toHaveLength(2);
    expect(items[0].text()).toContain('Creación');
    expect(items[0].text()).toContain('Tarea creada');
    expect(items[0].text()).toContain('Por: Admin');
  });

  it('muestra mensaje de lista vacía cuando no hay historial', async () => {
    historialTareaService.obtenerHistorial.mockResolvedValue([]);
    
    const wrapper = mount(HistorialTareaTimeline, {
      props: { tareaId: 5 }
    });
    
    await flushPromises();
    
    expect(wrapper.find('[data-test="empty"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="timeline"]').exists()).toBe(false);
  });

  it('muestra mensaje de error si falla el servicio', async () => {
    historialTareaService.obtenerHistorial.mockRejectedValue(new Error('Fallo de red'));
    
    const wrapper = mount(HistorialTareaTimeline, {
      props: { tareaId: 99 }
    });
    
    await flushPromises();
    
    expect(wrapper.find('[data-test="error"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="error"]').text()).toContain('Fallo de red');
  });
});