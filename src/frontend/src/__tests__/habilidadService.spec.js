import { describe, it, expect, vi, afterEach } from 'vitest'
import { habilidadService } from '../services/habilidadService'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

afterEach(() => vi.unstubAllGlobals())

describe('habilidadService', () => {
  it('listar consulta el catálogo de habilidades', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, [{ idHabilidad: 1, nombre: 'Cocina' }]))
    vi.stubGlobal('fetch', fetchMock)

    const habilidades = await habilidadService.listar()

    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/habilidades$/)
    expect(habilidades).toEqual([{ idHabilidad: 1, nombre: 'Cocina' }])
  })
})
