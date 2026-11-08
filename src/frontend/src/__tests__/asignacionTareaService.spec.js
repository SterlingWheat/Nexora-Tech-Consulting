import { describe, it, expect, vi, afterEach } from 'vitest'
import { asignacionTareaService } from '../services/asignacionTareaService'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

afterEach(() => vi.unstubAllGlobals())

describe('asignacionTareaService', () => {
  
  it('cancela una asignación con POST y conserva el recurso en el endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, null))
    vi.stubGlobal('fetch', fetchMock)

    await asignacionTareaService.cancelarAsignacion(10, 30)

    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/tareas\/10\/asignaciones\/30\/cancelar$/)
    expect(fetchMock.mock.calls[0][1].method).toBe('POST')
  })

  it('propaga el error al fallar la cancelación en el backend', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      respuesta(false, null, 'La asignación ya está cancelada o rechazada.')
    ))

    await expect(asignacionTareaService.cancelarAsignacion(10, 30))
      .rejects.toThrow('La asignación ya está cancelada o rechazada.')
  })

  it('reemplazarVoluntario envía un POST con el idNuevoVoluntario', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idAsignacion: 99 }))
    vi.stubGlobal('fetch', fetchMock)

    await asignacionTareaService.reemplazarVoluntario(10, 30, 5)

    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/tareas\/10\/asignaciones\/30\/reemplazar$/)
    expect(opciones.method).toBe('POST')
    expect(JSON.parse(opciones.body)).toEqual({ idNuevoVoluntario: 5 })
  })

})