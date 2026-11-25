import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}))

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: {
    buscarVoluntarios: vi.fn(),
    activarVoluntario: vi.fn(),
    desactivarVoluntario: vi.fn(),
    habilitarAcceso: vi.fn()
  },
}))

// HU2: Mock del catálogo de habilidades
vi.mock('../services/habilidadService', () => ({
  habilidadService: {
    obtenerHabilidades: vi.fn()
  }
}))

import VoluntariosView from '../views/VoluntariosView.vue'
import { voluntarioService } from '../services/voluntarioService'
import { habilidadService } from '../services/habilidadService'
import HistorialVoluntarioModal from '../components/HistorialVoluntarioModal.vue'

const pagina = (content, overrides = {}) => ({
  content,
  number: 0,
  totalPages: 1,
  totalElements: content.length,
  ...overrides,
})

describe('VoluntariosView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn().mockReturnValue(true))
    // Habilidades por defecto para que no falle al montar
    habilidadService.obtenerHabilidades.mockResolvedValue([
      { id: 1, nombre: 'Primeros Auxilios' },
      { id: 2, nombre: 'Liderazgo' }
    ])
  })

  it('lista los voluntarios con su estado', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([
      { idVoluntario: 1, nombre: 'Ana', apellido: 'Quispe', documento: '123', email: 'ana@correo.com', telefono: null, activo: true, fechaModificacion: null },
    ]))
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    expect(wrapper.text()).toContain('Ana Quispe')
    expect(wrapper.text()).toContain('Activo')
    expect(wrapper.text()).toContain('Sin teléfono')
  })

  it('busca al cargar sin filtros aplicados (incluyendo habilidadId)', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([]))
    mount(VoluntariosView)
    await flushPromises()

    expect(voluntarioService.buscarVoluntarios).toHaveBeenCalledWith({
      texto: undefined,
      activo: null,
      diaSemana: undefined,
      habilidadId: undefined,
      page: 0,
      size: 10,
    })
  })

  it('muestra mensaje vacío cuando no hay voluntarios registrados', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([]))
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    expect(wrapper.text()).toContain('No hay voluntarios registrados')
  })

  it('muestra el error si la API falla', async () => {
    voluntarioService.buscarVoluntarios.mockRejectedValue(new Error('Error al buscar voluntarios'))
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('Error al buscar')
  })

  it('navega a registrar y a editar', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([
      { idVoluntario: 3, nombre: 'A', apellido: 'B', documento: '1', email: 'a@b.co', activo: true },
    ]))
    const wrapper = mount(VoluntariosView)
    await flushPromises()

    await wrapper.find('.vols-boton-nuevo').trigger('click')
    expect(push).toHaveBeenCalledWith('/voluntarios/nuevo')

    await wrapper.find('.vols-boton-editar').trigger('click')
    expect(push).toHaveBeenCalledWith('/voluntarios/editar/3')
  })

  it('permite desactivar un voluntario, recargando la misma página', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([
      { idVoluntario: 1, nombre: 'Ana', apellido: 'Q', documento: '1', email: 'a@a.com', activo: true },
    ], { number: 2 }))
    voluntarioService.desactivarVoluntario.mockResolvedValue({})

    const wrapper = mount(VoluntariosView)
    await flushPromises()

    await wrapper.find('.btn-desactivar').trigger('click')
    await flushPromises()
    expect(voluntarioService.desactivarVoluntario).toHaveBeenCalledWith(1)
    expect(voluntarioService.buscarVoluntarios).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 2 })
    )
  })

  it('activar un voluntario pide confirmación en un modal propio antes de llamar al servicio', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([
      { idVoluntario: 2, nombre: 'Luis', apellido: 'Paz', documento: '2', email: 'l@l.com', activo: false }
    ]))
    voluntarioService.activarVoluntario.mockResolvedValue({})

    const wrapper = mount(VoluntariosView)
    await flushPromises()

    await wrapper.find('.btn-activar').trigger('click')
    expect(voluntarioService.activarVoluntario).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Luis Paz')
    expect(wrapper.find('.modal-contenido h2').text()).toBe('Activar voluntario')

    await wrapper.find('.modal-contenido .btn-guardar').trigger('click')
    await flushPromises()

    expect(voluntarioService.activarVoluntario).toHaveBeenCalledWith(2)
  })

  // KAN-27 Criterio 1
  it('busca por texto tras escribir, con debounce', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([]))
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    voluntarioService.buscarVoluntarios.mockClear()

    await wrapper.find('.vols-buscador').setValue('ana')
    expect(voluntarioService.buscarVoluntarios).not.toHaveBeenCalled()

    await new Promise((resolve) => setTimeout(resolve, 400))
    await flushPromises()

    expect(voluntarioService.buscarVoluntarios).toHaveBeenCalledWith(
      expect.objectContaining({ texto: 'ana', page: 0 })
    )
  })

  // KAN-27 Criterio 2
  it('aplica el filtro de estado activos/inactivos', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([]))
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    voluntarioService.buscarVoluntarios.mockClear()

    const botones = wrapper.findAll('.vols-filtro-boton')
    await botones.find((b) => b.text() === 'Activos').trigger('click')
    await flushPromises()
    expect(voluntarioService.buscarVoluntarios).toHaveBeenLastCalledWith(
      expect.objectContaining({ activo: true, page: 0 })
    )
  })

  // KAN-27 Criterio 2
  it('aplica el filtro de disponibilidad por día', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([]))
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    voluntarioService.buscarVoluntarios.mockClear()

    await wrapper.findAll('.vols-filtro-select')[0].setValue('LUNES')
    await flushPromises()

    expect(voluntarioService.buscarVoluntarios).toHaveBeenLastCalledWith(
      expect.objectContaining({ diaSemana: 'LUNES', page: 0 })
    )
  })

  // HU2: Criterio 2 - Filtrar voluntarios por habilidad
  it('aplica el filtro de habilidades', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([]))
    const wrapper = mount(VoluntariosView)
    await flushPromises()
    voluntarioService.buscarVoluntarios.mockClear()

    // El segundo select es el de habilidades
    await wrapper.findAll('.vols-filtro-select')[1].setValue('1')
    await flushPromises()

    expect(voluntarioService.buscarVoluntarios).toHaveBeenLastCalledWith(
      expect.objectContaining({ habilidadId: 1, page: 0 })
    )
  })

  // HU1: Criterio 1 - Abrir el historial
  it('abre el modal de historial al hacer clic en el botón Ver Historial', async () => {
    voluntarioService.buscarVoluntarios.mockResolvedValue(pagina([
      { idVoluntario: 5, nombre: 'Juan', apellido: 'Perez', documento: '1', email: 'j@p.com', activo: true }
    ]))
    const wrapper = mount(VoluntariosView)
    await flushPromises()

    // Encontrar el botón y abrir modal
    await wrapper.find('.vols-boton-historial').trigger('click')
    await flushPromises()

    const modal = wrapper.findComponent(HistorialVoluntarioModal)
    expect(modal.exists()).toBe(true)
    expect(modal.props('visible')).toBe(true)
    expect(modal.props('voluntario').idVoluntario).toBe(5)
  })
})