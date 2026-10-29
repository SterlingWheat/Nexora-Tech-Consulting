import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import RegistrarAvanceModal from '../components/RegistrarAvanceModal.vue'
import { tareaService } from '../services/tareaService'

vi.mock('../services/tareaService', () => ({
  tareaService: {
    registrarAvance: vi.fn()
  }
}))

describe('RegistrarAvanceModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('inicializa el formulario con el avance actual de la tarea', async () => {
    // Montamos el modal como oculto (estado inicial real)
    const wrapper = mount(RegistrarAvanceModal, {
      props: {
        visible: false,
        tarea: { idTarea: 1, titulo: 'Entregar donativos', avance: 45, estado: 'En progreso' }
      }
    })
    
    // Disparamos la apertura para que el 'watch' se ejecute
    await wrapper.setProps({ visible: true })
    await flushPromises()
    
    const inputAvance = wrapper.find('input[type="number"]')
    expect(inputAvance.element.value).toBe('45')
  })

  it('envía el registro de avance con la observación y emite eventos', async () => {
    tareaService.registrarAvance.mockResolvedValue({})
    
    const wrapper = mount(RegistrarAvanceModal, {
      props: {
        visible: false,
        tarea: { idTarea: 2, titulo: 'Ordenar almacén', avance: 10, estado: 'En progreso' }
      }
    })

    await wrapper.setProps({ visible: true })
    await flushPromises()

    await wrapper.find('input[type="number"]').setValue(80)
    await wrapper.find('textarea').setValue('El lote 2 está listo.')
    await wrapper.find('form').trigger('submit')
    
    await flushPromises()

    expect(tareaService.registrarAvance).toHaveBeenCalledWith(2, { 
      avance: 80, 
      observacion: 'El lote 2 está listo.' 
    })
    expect(wrapper.emitted()).toHaveProperty('avanceRegistrado')
    expect(wrapper.emitted()).toHaveProperty('close')
  })

  it('muestra el mensaje de error si el servicio falla', async () => {
    tareaService.registrarAvance.mockRejectedValue(new Error('La tarea debe estar "En progreso" para registrar avance'))
    
    const wrapper = mount(RegistrarAvanceModal, {
      props: {
        visible: false,
        tarea: { idTarea: 3, titulo: 'Tarea pendiente', avance: 0, estado: 'Pendiente' }
      }
    })

    await wrapper.setProps({ visible: true })
    await flushPromises()

    await wrapper.find('input[type="number"]').setValue(50)
    await wrapper.find('form').trigger('submit')
    
    await flushPromises()

    expect(wrapper.find('.modal-error').text()).toContain('La tarea debe estar "En progreso"')
    expect(wrapper.emitted('avanceRegistrado')).toBeFalsy()
  })
})