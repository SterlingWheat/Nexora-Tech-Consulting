import { describe, it, expect, vi, afterEach } from 'vitest'
import { disponibilidadService, DIAS } from '../services/disponibilidadService'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

afterEach(() => vi.unstubAllGlobals())

describe('disponibilidadService', () => {
  it('obtener consulta la disponibilidad del voluntario', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { franjas: [] }))
    vi.stubGlobal('fetch', fetchMock)
    await disponibilidadService.obtener(4)
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/voluntarios\/4\/disponibilidad$/)
  })

  it('guardar envía PUT con { franjas }', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { franjas: [], advertencias: [] }))
    vi.stubGlobal('fetch', fetchMock)
    const franjas = [{ diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '12:00' }]
    await disponibilidadService.guardar(4, franjas)
    const [, opciones] = fetchMock.mock.calls[0]
    expect(opciones.method).toBe('PUT')
    expect(JSON.parse(opciones.body)).toEqual({ franjas })
  })

  it('guardar propaga el mensaje de rechazo del backend (criterio 2)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, 'La hora de fin debe ser posterior')))
    await expect(disponibilidadService.guardar(4, [])).rejects.toThrow('La hora de fin debe ser posterior')
  })

  it('expone los siete días de la semana con los valores del backend', () => {
    expect(DIAS.map((d) => d.valor)).toEqual(['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'])
  })
})