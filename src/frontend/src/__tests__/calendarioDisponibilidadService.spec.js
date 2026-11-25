import { describe, it, expect, vi, afterEach } from 'vitest'
import { calendarioDisponibilidadService } from '../services/calendarioDisponibilidadService'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

afterEach(() => vi.unstubAllGlobals())

describe('calendarioDisponibilidadService', () => {
  it('obtenerCalendario envía desde y hasta como parámetros de consulta', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { desde: '2026-11-23', hasta: '2026-11-29', franjas: [] }))
    vi.stubGlobal('fetch', fetchMock)

    await calendarioDisponibilidadService.obtenerCalendario({ desde: '2026-11-23', hasta: '2026-11-29' })

    const url = fetchMock.mock.calls[0][0]
    expect(url).toMatch(/\/disponibilidad\/calendario\?/)
    expect(url).toMatch(/desde=2026-11-23/)
    expect(url).toMatch(/hasta=2026-11-29/)
    expect(url).not.toMatch(/idVoluntario/)
    expect(url).not.toMatch(/idHabilidad/)
  })

  it('obtenerCalendario incluye idVoluntario e idHabilidad cuando se filtran (criterio 2)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { franjas: [] }))
    vi.stubGlobal('fetch', fetchMock)

    await calendarioDisponibilidadService.obtenerCalendario({
      desde: '2026-11-23', hasta: '2026-11-29', idVoluntario: 5, idHabilidad: 3
    })

    const url = fetchMock.mock.calls[0][0]
    expect(url).toMatch(/idVoluntario=5/)
    expect(url).toMatch(/idHabilidad=3/)
  })

  it('propaga el mensaje de error del backend', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, "La fecha 'hasta' debe ser posterior a 'desde'")))

    await expect(calendarioDisponibilidadService.obtenerCalendario({ desde: '2026-11-29', hasta: '2026-11-23' }))
      .rejects.toThrow("La fecha 'hasta' debe ser posterior a 'desde'")
  })
})
