import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest'
import { reporteService, TIPOS_REPORTE } from '../services/reporteService'

const respuestaBlob = (ok, blob, texto = '', status = ok ? 200 : 400) => ({
  ok, status, blob: async () => blob, text: async () => texto
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('reporteService', () => {
  it('expone los tres tipos de reporte del dominio', () => {
    expect(TIPOS_REPORTE.map(r => r.valor)).toEqual(['voluntarios', 'tareas', 'turnos'])
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
