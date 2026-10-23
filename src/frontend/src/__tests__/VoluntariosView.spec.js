import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}))

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: { 
    obtenerVoluntarios: vi.fn(),
    activarVoluntario: vi.fn(),
    desactivarVoluntario: vi.fn(),
    habilitarAcceso: vi.fn()
  },
}))

import VoluntariosView from '../views/VoluntariosView.vue'
import { voluntarioService } from '../services/voluntarioService'

describe('VoluntariosView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn().mockReturnValue(true))
  })

  it('lista los voluntarios con su estado', async () => {
    voluntarioService.obtenerVoluntarios.mockResolvedValue([
      { idVoluntario: 1, nombre: 'Ana', apellido: 'Quispe', documento: '123', email: 'ana@correo.com', telefono: null, activo: true, fechaModificacion: null },
    ])
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    expect(wrapper.text()).toContain('Ana Quispe')
    expect(wrapper.text()).toContain('Activo')
    expect(wrapper.text()).toContain('Sin teléfono')
  })

  it('muestra mensaje vacío cuando no hay voluntarios', async () => {
    voluntarioService.obtenerVoluntarios.mockResolvedValue([])
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    expect(wrapper.text()).toContain('No hay voluntarios registrados')
  })

  it('muestra el error si la API falla', async () => {
    voluntarioService.obtenerVoluntarios.mockRejectedValue(new Error('Error al obtener los voluntarios'))
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('Error al obtener')
  })

  it('navega a registrar y a editar', async () => {
    voluntarioService.obtenerVoluntarios.mockResolvedValue([
      { idVoluntario: 3, nombre: 'A', apellido: 'B', documento: '1', email: 'a@b.co', activo: true },
    ])
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    
    await wrapper.find('.vols-boton-nuevo').trigger('click')
    expect(push).toHaveBeenCalledWith('/voluntarios/nuevo')

    await wrapper.find('.vols-boton-editar').trigger('click')
    expect(push).toHaveBeenCalledWith('/voluntarios/editar/3')
  })

  it('permite desactivar y activar un voluntario', async () => {
    voluntarioService.obtenerVoluntarios.mockResolvedValue([
      { idVoluntario: 1, nombre: 'Ana', apellido: 'Q', documento: '1', email: 'a@a.com', activo: true },
      { idVoluntario: 2, nombre: 'Luis', apellido: 'P', documento: '2', email: 'l@l.com', activo: false }
    ])
    voluntarioService.desactivarVoluntario.mockResolvedValue({})
    voluntarioService.activarVoluntario.mockResolvedValue({})

    const wrapper = mount(VoluntariosView)
    await flushPromises()

    await wrapper.find('.btn-desactivar').trigger('click')
    await flushPromises()
    expect(voluntarioService.desactivarVoluntario).toHaveBeenCalledWith(1)

    await wrapper.find('.btn-activar').trigger('click')
    await flushPromises()
    expect(voluntarioService.activarVoluntario).toHaveBeenCalledWith(2)
  })
})