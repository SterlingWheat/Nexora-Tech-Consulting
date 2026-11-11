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

  it('actualizarTurno usa PUT para persistir cambios de horario', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idTurno: 1 }))
    vi.stubGlobal('fetch', fetchMock)
    await turnoService.actualizarTurno(1, turno)
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/turnos\/1$/)
    expect(fetchMock.mock.calls[0][1].method).toBe('PUT')
  })

  it('asignarVoluntarios conserva la lista seleccionada en el request', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, [{ idAsignacion: 10 }]))
    vi.stubGlobal('fetch', fetchMock)
    await turnoService.asignarVoluntarios(1, [5, 6])
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/turnos\/1\/asignaciones$/)
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ idVoluntarios: [5, 6] })
  })

  it('cancelarTurno persiste el estado y no elimina el turno', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idTurno: 1, estado: 'CANCELADO' }))
    vi.stubGlobal('fetch', fetchMock)
    const resultado = await turnoService.cancelarTurno(1)
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/turnos\/1\/cancelar$/)
    expect(fetchMock.mock.calls[0][1].method).toBe('POST')
    expect(resultado.estado).toBe('CANCELADO')
  })

  it('obtenerTurnos devuelve la lista con cupos (criterio 3)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(true, [{ idTurno: 1, cuposOcupados: 2, cuposLibres: 3 }])))
    const turnos = await turnoService.obtenerTurnos()
    expect(turnos[0]).toMatchObject({ cuposOcupados: 2, cuposLibres: 3 })
  })

  // HU2 Criterio 1: Confirmar asignación
  it('confirmarAsignacion usa PATCH para actualizar el estado', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, null))
    vi.stubGlobal('fetch', fetchMock)
    await turnoService.confirmarAsignacion(1, 30)
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/turnos\/1\/asignaciones\/30\/confirmar$/)
    expect(fetchMock.mock.calls[0][1].method).toBe('PATCH')
  })

  // HU2 Criterios 2 y 3: Cancelar asignación
  it('cancelarAsignacion usa POST para cancelar la asignación', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, null))
    vi.stubGlobal('fetch', fetchMock)
    await turnoService.cancelarAsignacion(1, 30)
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/turnos\/1\/asignaciones\/30\/cancelar$/)
    expect(fetchMock.mock.calls[0][1].method).toBe('POST')
  })
})