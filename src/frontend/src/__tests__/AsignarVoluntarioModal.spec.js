import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AsignarVoluntarioModal from '../components/AsignarVoluntarioModal.vue'
import { voluntarioService } from '../services/voluntarioService'
import { asignacionTareaService } from '../services/asignacionTareaService'

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: { obtenerVoluntarios: vi.fn() }
}))

vi.mock('../services/asignacionTareaService', () => ({
  asignacionTareaService: { asignarVoluntarios: vi.fn() }
}))

describe('AsignarVoluntarioModal', () => {
  const listaVoluntarios = [
    { idVoluntario: 1, nombre: 'Ana', apellido: 'Quispe', documento: '123', activo: true },
    { idVoluntario: 2, nombre: 'Carlos', apellido: 'Perez', documento: '456', activo: false }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    voluntarioService.obtenerVoluntarios.mockResolvedValue(listaVoluntarios)
  })

  it('filtra y muestra únicamente a los voluntarios activos (HU2 Criterio 2)', async () => {
    // Montar en false y luego pasar a true dispara el "watch" que carga los voluntarios
    const wrapper = mount(AsignarVoluntarioModal, {
      props: { visible: false, tarea: { idTarea: 10, titulo: 'Reparto' } }
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect(wrapper.text()).toContain('Ana Quispe')
    expect(wrapper.text()).not.toContain('Carlos Perez')
  })

  it('el botón de guardar permanece deshabilitado hasta seleccionar al menos uno', async () => {
    const wrapper = mount(AsignarVoluntarioModal, {
      props: { visible: false, tarea: { idTarea: 10, titulo: 'Reparto' } }
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    const btnGuardar = wrapper.find('.btn-guardar')
    expect(btnGuardar.element.disabled).toBe(true)

    const checkbox = wrapper.find('input[type="checkbox"]')
    await checkbox.setValue(true)

    expect(btnGuardar.element.disabled).toBe(false)
  })

  it('asigna los voluntarios seleccionados y emite evento asignado (HU2 Criterio 1)', async () => {
    asignacionTareaService.asignarVoluntarios.mockResolvedValue([])

    const wrapper = mount(AsignarVoluntarioModal, {
      props: { visible: false, tarea: { idTarea: 10, titulo: 'Reparto' } }
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    await wrapper.find('input[type="checkbox"]').setValue(true)
    await wrapper.find('.btn-guardar').trigger('click')
    await flushPromises()

    expect(asignacionTareaService.asignarVoluntarios).toHaveBeenCalledWith(10, [1])
    expect(wrapper.emitted()).toHaveProperty('asignado')
    expect(wrapper.emitted()).toHaveProperty('close')
  })
})