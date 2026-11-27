import { describe, it, expect, vi, afterEach } from 'vitest'
import { reporteService, TIPOS_REPORTE } from '../services/reporteService'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

afterEach(() => {
  vi.unstubAllGlobals()
})

// RF-45/46 (KAN-56)
describe('reporteService', () => {
  it('expone los tres tipos de reporte del dominio', () => {
    expect(TIPOS_REPORTE.map(r => r.valor)).toEqual(['voluntarios', 'tareas', 'turnos'])
  })

  it('obtenerReporte envía GET al tipo de reporte indicado y devuelve el JSON', async () => {
    const reporteFalso = {
      titulo: 'Reporte de voluntarios',
      resumen: { Total: '2', Activos: '1' },
      encabezados: ['Nombre', 'Activo'],
      filas: [['Ana', true]]
    }
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, reporteFalso))
    vi.stubGlobal('fetch', fetchMock)

    const reporte = await reporteService.obtenerReporte('voluntarios')

    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/reportes\/voluntarios$/)
    expect(reporte).toEqual(reporteFalso)
  })

  it('propaga el error cuando el backend responde con un tipo de reporte inválido', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, 'Tipo de reporte no soportado: inexistente')))

    await expect(reporteService.obtenerReporte('inexistente'))
      .rejects.toThrow('Tipo de reporte no soportado: inexistente')
  })
})
