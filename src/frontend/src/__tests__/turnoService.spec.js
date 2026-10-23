import { describe, it, expect, vi, afterEach } from 'vitest'
import { turnoService } from '../services/turnoService'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

afterEach(() => vi.unstubAllGlobals())

const turno = { fecha: '2026-10-25', horaInicio: '08:00', horaFin: '12:00', actividad: 'Clasificar', cuposTotales: 5 }

describe('turnoService', () => {
  it('crearTurno envía POST con el turno (criterio 1)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idTurno: 1, cuposLibres: 5 }))
    vi.stubGlobal('fetch', fetchMock)
    const creado = await turnoService.crearTurno(turno)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/turnos$/)
    expect(opciones.method).toBe('POST')
    expect(JSON.parse(opciones.body)).toEqual(turno)
    expect(creado.cuposLibres).toBe(5)
  })

  it('crearTurno propaga el rechazo del backend (criterio 2)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, 'La fecha del turno no puede ser pasada')))
    await expect(turnoService.crearTurno(turno)).rejects.toThrow('La fecha del turno no puede ser pasada')
  })

  it('validarCruce consulta el endpoint del voluntario y turno (KAN-22)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { permitido: true }))
    vi.stubGlobal('fetch', fetchMock)
    const resultado = await turnoService.validarCruce(5, 2)
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/voluntarios\/5\/turnos\/2\/validacion-cruce$/)
    expect(resultado.permitido).toBe(true)
  })

  it('validarCruce muestra el turno con el que choca cuando hay cruce (KAN-22)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, 'Cruce de horarios: el voluntario ya está asignado a Clasificar')))
    await expect(turnoService.validarCruce(5, 2)).rejects.toThrow('Clasificar')
  })

  it('obtenerTurnos devuelve la lista con cupos (criterio 3)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(true, [{ idTurno: 1, cuposOcupados: 2, cuposLibres: 3 }])))
    const turnos = await turnoService.obtenerTurnos()
    expect(turnos[0]).toMatchObject({ cuposOcupados: 2, cuposLibres: 3 })
  })
})