import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest'
import { reporteService, TIPOS_REPORTE } from '../services/reporteService'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

const respuestaBlob = (ok, blob, texto = '', status = ok ? 200 : 400) => ({
  ok, status, blob: async () => blob, text: async () => texto
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

// RF-45/46/47 (KAN-56/58)
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

  describe('exportarPdf', () => {
    let clicSpy
    let createObjectURLSpy
    let revokeObjectURLSpy

    beforeEach(() => {
      clicSpy = vi.fn()
      const crearElementoOriginal = document.createElement.bind(document)
      vi.spyOn(document, 'createElement').mockImplementation((tag) => {
        const elemento = crearElementoOriginal(tag)
        elemento.click = clicSpy
        return elemento
      })
      createObjectURLSpy = vi.fn().mockReturnValue('blob:falso')
      revokeObjectURLSpy = vi.fn()
      vi.stubGlobal('URL', { createObjectURL: createObjectURLSpy, revokeObjectURL: revokeObjectURLSpy })
    })

    it('descarga el PDF del tipo de reporte indicado', async () => {
      const blobFalso = new Blob(['pdf'])
      const fetchMock = vi.fn().mockResolvedValue(respuestaBlob(true, blobFalso))
      vi.stubGlobal('fetch', fetchMock)

      await reporteService.exportarPdf('voluntarios')

      expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/reportes\/voluntarios\/pdf$/)
      expect(createObjectURLSpy).toHaveBeenCalledWith(blobFalso)
      expect(clicSpy).toHaveBeenCalledTimes(1)
      expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:falso')
    })

    it('propaga el error cuando la exportación falla', async () => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuestaBlob(false, null, 'No se pudo exportar')))
      await expect(reporteService.exportarPdf('tareas')).rejects.toThrow('No se pudo exportar')
    })

    it('descarga el Excel del tipo de reporte indicado', async () => {
      const blobFalso = new Blob(['xlsx'])
      const fetchMock = vi.fn().mockResolvedValue(respuestaBlob(true, blobFalso))
      vi.stubGlobal('fetch', fetchMock)

      await reporteService.exportarExcel('turnos')

      expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/reportes\/turnos\/excel$/)
      expect(clicSpy).toHaveBeenCalledTimes(1)
    })
  })
})
