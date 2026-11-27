import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ReportesView from '../views/ReportesView.vue'
import { reporteService } from '../services/reporteService'

// RF-47 (KAN-58)
vi.mock('../services/reporteService', () => ({
  reporteService: {
    exportarPdf: vi.fn(),
    exportarExcel: vi.fn()
  },
  TIPOS_REPORTE: [
    { valor: 'voluntarios', etiqueta: 'Voluntarios' },
    { valor: 'tareas', etiqueta: 'Tareas' },
    { valor: 'turnos', etiqueta: 'Turnos' }
  ]
}))

describe('ReportesView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra los tres tipos de reporte con sus botones de exportación', () => {
    const wrapper = mount(ReportesView)

    expect(wrapper.text()).toContain('Voluntarios')
    expect(wrapper.text()).toContain('Tareas')
    expect(wrapper.text()).toContain('Turnos')
    expect(wrapper.findAll('button').length).toBe(6)
  })

  it('exporta a PDF el reporte de voluntarios al pulsar el botón', async () => {
    reporteService.exportarPdf.mockResolvedValue()
    const wrapper = mount(ReportesView)

    const botonPdf = wrapper.findAll('button').find(b => b.text() === 'Exportar PDF')
    await botonPdf.trigger('click')
    await flushPromises()

    expect(reporteService.exportarPdf).toHaveBeenCalledWith('voluntarios')
  })

  it('exporta a Excel el reporte de tareas al pulsar su botón', async () => {
    reporteService.exportarExcel.mockResolvedValue()
    const wrapper = mount(ReportesView)

    const filas = wrapper.findAll('.reporte-fila')
    const botonExcelTareas = filas[1].findAll('button').find(b => b.text().includes('Excel'))
    await botonExcelTareas.trigger('click')
    await flushPromises()

    expect(reporteService.exportarExcel).toHaveBeenCalledWith('tareas')
  })

  it('muestra un mensaje de error cuando la exportación falla', async () => {
    reporteService.exportarPdf.mockRejectedValue(new Error('No se pudo exportar el reporte'))
    const wrapper = mount(ReportesView)

    const botonPdf = wrapper.findAll('button').find(b => b.text() === 'Exportar PDF')
    await botonPdf.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('No se pudo exportar el reporte')
  })
})
