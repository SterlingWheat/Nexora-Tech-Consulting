import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: { obtenerVoluntarios: vi.fn() },
}))

vi.mock('../services/turnoService', () => ({
  turnoService: { obtenerCalendario: vi.fn() },
}))

import CalendarioTurnosView from '../views/CalendarioTurnosView.vue'
import { voluntarioService } from '../services/voluntarioService'
import { turnoService } from '../services/turnoService'

const VOLUNTARIOS = [{ idVoluntario: 1, nombre: 'Ana', apellido: 'Pérez' }]

async function montar(turnos = []) {
  voluntarioService.obtenerVoluntarios.mockResolvedValue(VOLUNTARIOS)
  turnoService.obtenerCalendario.mockResolvedValue(turnos)
  const wrapper = mount(CalendarioTurnosView)
  await flushPromises()
  return wrapper
}

describe('CalendarioTurnosView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    // Miércoles 2026-11-25: la semana de esa fecha va del lunes 23 al domingo 29
    vi.setSystemTime(new Date(2026, 10, 25))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  // Criterio 1: vista semana por defecto, consulta el rango lunes-domingo
  it('al cargar, en vista semana consulta el rango lunes-domingo de la semana actual', async () => {
    await montar([])

    expect(turnoService.obtenerCalendario).toHaveBeenCalledWith(
      expect.objectContaining({ desde: '2026-11-23', hasta: '2026-11-29' })
    )
  })

  // Criterio 1: cambiar a vista día reduce el rango a un solo día
  it('cambiar a la vista Día consulta solo el día actual', async () => {
    const wrapper = await montar([])

    await wrapper.findAll('.calt-vista-boton').find((b) => b.text() === 'Día').trigger('click')
    await flushPromises()

    expect(turnoService.obtenerCalendario).toHaveBeenLastCalledWith(
      expect.objectContaining({ desde: '2026-11-25', hasta: '2026-11-25' })
    )
  })

  // Criterio 1: cambiar a vista mes amplía el rango a las semanas completas del mes
  it('cambiar a la vista Mes consulta un rango que cubre todo noviembre', async () => {
    const wrapper = await montar([])

    await wrapper.findAll('.calt-vista-boton').find((b) => b.text() === 'Mes').trigger('click')
    await flushPromises()

    const llamada = turnoService.obtenerCalendario.mock.calls.at(-1)[0]
    expect(llamada.desde <= '2026-11-01').toBe(true)
    expect(llamada.hasta >= '2026-11-30').toBe(true)
  })

  // Criterio 2: un turno con cupos sin cubrir se resalta como incompleto
  it('un turno con cupos libres se marca como incompleto', async () => {
    const wrapper = await montar([
      { idTurno: 1, fecha: '2026-11-23', horaInicio: '08:00:00', horaFin: '12:00:00', actividad: 'Reparto', cuposTotales: 5, cuposOcupados: 2, cuposLibres: 3 },
    ])

    const tarjeta = wrapper.find('.calt-turno-tarjeta')
    expect(tarjeta.classes()).toContain('incompleto')
    expect(tarjeta.text()).toContain('Reparto')
  })

  // Criterio 2: un turno completamente cubierto no se marca como incompleto
  it('un turno sin cupos libres se marca como completo', async () => {
    const wrapper = await montar([
      { idTurno: 2, fecha: '2026-11-23', horaInicio: '08:00:00', horaFin: '12:00:00', actividad: 'Reparto', cuposTotales: 5, cuposOcupados: 5, cuposLibres: 0 },
    ])

    const tarjeta = wrapper.find('.calt-turno-tarjeta')
    expect(tarjeta.classes()).toContain('completo')
    expect(tarjeta.classes()).not.toContain('incompleto')
  })

  it('un día sin turnos muestra el mensaje de "sin turnos"', async () => {
    const wrapper = await montar([])
    expect(wrapper.findAll('.calt-sin-turnos').length).toBeGreaterThan(0)
  })

  // Filtro por actividad y por voluntario (subtarea KAN-125/126)
  it('al escribir una actividad, vuelve a consultar con ese filtro', async () => {
    const wrapper = await montar([])

    await wrapper.find('.calt-filtro input').setValue('alimentos')
    await flushPromises()

    expect(turnoService.obtenerCalendario).toHaveBeenLastCalledWith(
      expect.objectContaining({ actividad: 'alimentos' })
    )
  })

  it('al elegir un voluntario en el filtro, vuelve a consultar con ese filtro', async () => {
    const wrapper = await montar([])

    await wrapper.find('.calt-filtro select').setValue(1)
    await flushPromises()

    expect(turnoService.obtenerCalendario).toHaveBeenLastCalledWith(
      expect.objectContaining({ idVoluntario: 1 })
    )
  })

  it('muestra el error devuelto por el backend', async () => {
    voluntarioService.obtenerVoluntarios.mockResolvedValue(VOLUNTARIOS)
    turnoService.obtenerCalendario.mockRejectedValue(new Error('El rango del calendario no puede superar los 180 días'))

    const wrapper = mount(CalendarioTurnosView)
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('El rango del calendario no puede superar')
  })
})
