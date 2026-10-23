import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import CambiarEstadoModal from '../components/CambiarEstadoModal.vue'
import { tareaService } from '../services/tareaService'

vi.mock('../services/tareaService', () => ({
  tareaService: {
    cambiarEstado: vi.fn()
  }
}))

describe('CambiarEstadoModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('no muestra estados a elegir si la tarea ya está Finalizada (HU3 Criterio 3)', () => {
    const wrapper = mount(CambiarEstadoModal, {
      props: {
        visible: true,
        tarea: { idTarea: 1, titulo: 'Tarea Concluida', estado: 'Finalizada' }
      }
    })

    const opciones = wrapper.findAll('select option')
    expect(opciones).toHaveLength(1)
    expect(opciones[0].element.disabled).toBe(true)
  })

  it('permite cambiar el estado de Pendiente a En progreso y emite eventos', async () => {
    tareaService.cambiarEstado.mockResolvedValue({ idTarea: 2, estado: 'En progreso' })

    const wrapper = mount(CambiarEstadoModal, {
      props: {
        visible: true,
        tarea: { idTarea: 2, titulo: 'Ordenar almacén', estado: 'Pendiente' }
      }
    })

    await wrapper.find('select').setValue('En progreso')
    await wrapper.find('.btn-guardar').trigger('click')
    await flushPromises()

    expect(tareaService.cambiarEstado).toHaveBeenCalledWith(2, 'En progreso')
    expect(wrapper.emitted()).toHaveProperty('estadoCambiado')
    expect(wrapper.emitted()).toHaveProperty('close')
  })

  it('muestra el mensaje de error si el servicio falla', async () => {
    tareaService.cambiarEstado.mockRejectedValue(new Error('Error de validación'))

    const wrapper = mount(CambiarEstadoModal, {
      props: {
        visible: true,
        tarea: { idTarea: 2, titulo: 'Tarea X', estado: 'Pendiente' }
      }
    })

    await wrapper.find('select').setValue('En progreso')
    await wrapper.find('.btn-guardar').trigger('click')
    await flushPromises()

    expect(wrapper.find('.modal-error').text()).toContain('Error de validación')
  })
})