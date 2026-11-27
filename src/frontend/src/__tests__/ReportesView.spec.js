import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ReportesView from '../views/ReportesView.vue'
import { reporteService } from '../services/reporteService'

// RF-45/46/47 (KAN-56/58)
vi.mock('../services/reporteService', () => ({
  reporteService: {
    obtenerReporte: vi.fn(),
    exportarPdf: vi.fn(),
    exportarExcel: vi.fn()
  },
  TIPOS_REPORTE: [
    { valor: 'voluntarios', etiqueta: 'Voluntarios' },
    { valor: 'tareas', etiqueta: 'Tareas' },
    { valor: 'turnos', etiqueta: 'Turnos' }
  ]
}))

const reporteVoluntarios = {
  titulo: 'Reporte de voluntarios',
  resumen: { Total: '2', Activos: '1', Inactivos: '1' },
  encabezados: ['Nombre', 'Activo'],
  filas: [['Ana', true], ['Beto', false]]
}

const reporteTareas = {
  titulo: 'Reporte de tareas',
  resumen: { Total: '1', Pendiente: '1' },
  encabezados: ['Título', 'Estado'],
  filas: [['Armar kits', 'Pendiente']]
}

describe('ReportesView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('carga el reporte de voluntarios al montar y muestra el resumen y la tabla', async () => {
    reporteService.obtenerReporte.mockResolvedValue(reporteVoluntarios)

    const wrapper = mount(ReportesView)
    await flushPromises()

    expect(reporteService.obtenerReporte).toHaveBeenCalledWith('voluntarios')
    expect(wrapper.text()).toContain('Reporte de voluntarios')
    expect(wrapper.text()).toContain('Activos')
    expect(wrapper.text()).toContain('Ana')
    expect(wrapper.text()).toContain('Sí')
    expect(wrapper.text()).toContain('No')
  })

  it('cambia de reporte al seleccionar otro tipo', async () => {
    reporteService.obtenerReporte.mockResolvedValueOnce(reporteVoluntarios)
    reporteService.obtenerReporte.mockResolvedValueOnce(reporteTareas)

    const wrapper = mount(ReportesView)
    await flushPromises()

    const botonTareas = wrapper.findAll('button').find(b => b.text() === 'Tareas')
    await botonTareas.trigger('click')
    await flushPromises()

    expect(reporteService.obtenerReporte).toHaveBeenCalledWith('tareas')
    expect(wrapper.text()).toContain('Reporte de tareas')
    expect(wrapper.text()).toContain('Armar kits')
  })

  it('muestra un mensaje cuando no hay filas', async () => {
    reporteService.obtenerReporte.mockResolvedValue({
      titulo: 'Reporte de turnos',
      resumen: { 'Total turnos': '0' },
      encabezados: ['Fecha'],
      filas: []
    })

    const wrapper = mount(ReportesView)
    await flushPromises()

    expect(wrapper.text()).toContain('No hay datos para este reporte.')
  })

  it('muestra un mensaje de error cuando la carga falla', async () => {
    reporteService.obtenerReporte.mockRejectedValue(new Error('Error al obtener el reporte'))

    const wrapper = mount(ReportesView)
    await flushPromises()

    expect(wrapper.text()).toContain('Error al obtener el reporte')
  })

  it('exporta a PDF el reporte seleccionado al pulsar el botón', async () => {
    reporteService.obtenerReporte.mockResolvedValue(reporteVoluntarios)
    reporteService.exportarPdf.mockResolvedValue()

    const wrapper = mount(ReportesView)
    await flushPromises()

    const botonPdf = wrapper.findAll('button').find(b => b.text() === 'Exportar PDF')
    await botonPdf.trigger('click')
    await flushPromises()

    expect(reporteService.exportarPdf).toHaveBeenCalledWith('voluntarios')
  })

  it('exporta a Excel el reporte seleccionado al pulsar el botón', async () => {
    reporteService.obtenerReporte.mockResolvedValueOnce(reporteVoluntarios)
    reporteService.obtenerReporte.mockResolvedValueOnce(reporteTareas)
    reporteService.exportarExcel.mockResolvedValue()

    const wrapper = mount(ReportesView)
    await flushPromises()

    const botonTareas = wrapper.findAll('button').find(b => b.text() === 'Tareas')
    await botonTareas.trigger('click')
    await flushPromises()

    const botonExcel = wrapper.findAll('button').find(b => b.text() === 'Exportar Excel')
    await botonExcel.trigger('click')
    await flushPromises()

    expect(reporteService.exportarExcel).toHaveBeenCalledWith('tareas')
  })

  it('muestra un mensaje de error cuando la exportación falla', async () => {
    reporteService.obtenerReporte.mockResolvedValue(reporteVoluntarios)
    reporteService.exportarPdf.mockRejectedValue(new Error('No se pudo exportar el reporte'))

    const wrapper = mount(ReportesView)
    await flushPromises()

    const botonPdf = wrapper.findAll('button').find(b => b.text() === 'Exportar PDF')
    await botonPdf.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('No se pudo exportar el reporte')
  })
})
