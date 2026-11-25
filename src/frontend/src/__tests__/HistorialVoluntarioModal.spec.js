import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import HistorialVoluntarioModal from '../components/HistorialVoluntarioModal.vue'
import { request } from '../services/apiClient'

// Mock directo a la función request de apiClient
vi.mock('../services/apiClient', () => ({
  request: vi.fn()
}))

describe('HistorialVoluntarioModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const voluntarioProp = { idVoluntario: 1, nombre: 'Carlos', apellido: 'Mesa' }

  it('no renderiza su contenido si visible es false', () => {
    const wrapper = mount(HistorialVoluntarioModal, {
      props: { visible: false, voluntario: voluntarioProp }
    })
    expect(wrapper.find('.modal-overlay').exists()).toBe(false)
    expect(request).not.toHaveBeenCalled()
  })

  it('carga y muestra el historial al hacerse visible', async () => {
    request.mockResolvedValue([
      { participacionId: 10, tipo: 'TAREA', nombre: 'Limpieza', fecha: '2026-11-20', estado: 'COMPLETADA' },
      { participacionId: 11, tipo: 'TURNO', nombre: 'Turno Mañana', fecha: '2026-11-21', estado: 'CANCELADA' }
    ])

    const wrapper = mount(HistorialVoluntarioModal, {
      props: { visible: true, voluntario: voluntarioProp }
    })
    await flushPromises()

    expect(request).toHaveBeenCalledWith(
      '/voluntarios/1/historial?', 
      expect.any(Object)
    )
    expect(wrapper.text()).toContain('Limpieza')
    expect(wrapper.text()).toContain('Turno Mañana')
    expect(wrapper.text()).toContain('COMPLETADA')
    expect(wrapper.text()).toContain('CANCELADA')
  })

  it('muestra mensaje cuando el historial está vacío', async () => {
    request.mockResolvedValue([])

    const wrapper = mount(HistorialVoluntarioModal, {
      props: { visible: true, voluntario: voluntarioProp }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No hay registros de participación')
  })

  it('emite evento close al hacer clic en el botón de cerrar', async () => {
    request.mockResolvedValue([])

    const wrapper = mount(HistorialVoluntarioModal, {
      props: { visible: true, voluntario: voluntarioProp }
    })
    await flushPromises()

    await wrapper.find('.btn-cerrar').trigger('click')
    expect(wrapper.emitted().close).toBeTruthy()
    expect(wrapper.emitted().close.length).toBe(1)
  })

  it('reacciona a los cambios en los filtros de fecha disparando una nueva petición', async () => {
    request.mockResolvedValue([])

    const wrapper = mount(HistorialVoluntarioModal, {
      props: { visible: true, voluntario: voluntarioProp }
    })
    await flushPromises()
    request.mockClear() // Limpiamos la llamada de montaje inicial

    // Seteamos fechaDesde
    await wrapper.find('#fechaDesde').setValue('2026-11-01')
    await flushPromises()

    expect(request).toHaveBeenCalledTimes(1)
    expect(request.mock.calls[0][0]).toContain('desde=2026-11-01')

    // Seteamos fechaHasta
    await wrapper.find('#fechaHasta').setValue('2026-11-30')
    await flushPromises()

    expect(request).toHaveBeenCalledTimes(2)
    expect(request.mock.calls[1][0]).toContain('desde=2026-11-01')
    expect(request.mock.calls[1][0]).toContain('hasta=2026-11-30')
  })

  it('limpia los filtros al usar el botón de limpiar', async () => {
    request.mockResolvedValue([])

    const wrapper = mount(HistorialVoluntarioModal, {
      props: { visible: true, voluntario: voluntarioProp }
    })
    await flushPromises()

    await wrapper.find('#fechaDesde').setValue('2026-11-01')
    await flushPromises()
    
    // Hacer clic en limpiar
    await wrapper.find('.btn-limpiar').trigger('click')
    await flushPromises()

    expect(wrapper.find('#fechaDesde').element.value).toBe('')
    // La reactividad disparó una nueva petición sin parámetros de query
    expect(request.mock.calls[request.mock.calls.length - 1][0]).toBe('/voluntarios/1/historial?')
  })
})