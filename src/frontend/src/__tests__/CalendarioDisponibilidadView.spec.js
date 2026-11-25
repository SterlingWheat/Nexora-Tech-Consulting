import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: { obtenerVoluntarios: vi.fn() },
}))

vi.mock('../services/habilidadService', () => ({
  habilidadService: { obtenerHabilidades: vi.fn() },
}))

vi.mock('../services/calendarioDisponibilidadService', () => ({
  calendarioDisponibilidadService: { obtenerCalendario: vi.fn() },
}))

import CalendarioDisponibilidadView from '../views/CalendarioDisponibilidadView.vue'
import { voluntarioService } from '../services/voluntarioService'
import { habilidadService } from '../services/habilidadService'
import { calendarioDisponibilidadService } from '../services/calendarioDisponibilidadService'

const VOLUNTARIOS = [
  { idVoluntario: 1, nombre: 'Ana', apellido: 'Pérez' },
  { idVoluntario: 2, nombre: 'Ben', apellido: 'Diaz' },
]
const HABILIDADES = [{ id: 3, nombre: 'Cocina' }]

async function montar(franjas = []) {
  voluntarioService.obtenerVoluntarios.mockResolvedValue(VOLUNTARIOS)
  habilidadService.obtenerHabilidades.mockResolvedValue(HABILIDADES)
  calendarioDisponibilidadService.obtenerCalendario.mockResolvedValue({
    desde: '2026-11-23', hasta: '2026-11-29', franjas,
  })
  const wrapper = mount(CalendarioDisponibilidadView)
  await flushPromises()
  return wrapper
}

describe('CalendarioDisponibilidadView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    // Miércoles 2026-11-25: la semana de esa fecha va del lunes 23 al domingo 29
    vi.setSystemTime(new Date(2026, 10, 25))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  // Criterio 1
  it('al cargar, consulta el rango de la semana actual y muestra las franjas por voluntario', async () => {
    const wrapper = await montar([
      { idVoluntario: 1, nombreVoluntario: 'Ana', apellidoVoluntario: 'Pérez', fecha: '2026-11-23', diaSemana: 'LUNES', horaInicio: '08:00:00', horaFin: '12:00:00' },
    ])

    expect(calendarioDisponibilidadService.obtenerCalendario).toHaveBeenCalledWith(
      expect.objectContaining({ desde: '2026-11-23', hasta: '2026-11-29' })
    )
    expect(wrapper.text()).toContain('Ana Pérez')
    expect(wrapper.text()).toContain('08:00')
    expect(wrapper.text()).toContain('12:00')
  })

  it('un día sin franjas muestra el mensaje de "sin disponibilidad"', async () => {
    const wrapper = await montar([])
    expect(wrapper.findAll('.cal-sin-franjas').length).toBeGreaterThan(0)
  })

  // Criterio 1: cambiar de semana vuelve a consultar con el nuevo rango
  it('la navegación a la semana siguiente vuelve a consultar con el nuevo rango', async () => {
    const wrapper = await montar([])

    await wrapper.find('[aria-label="Semana siguiente"]').trigger('click')
    await flushPromises()

    expect(calendarioDisponibilidadService.obtenerCalendario).toHaveBeenLastCalledWith(
      expect.objectContaining({ desde: '2026-11-30', hasta: '2026-12-06' })
    )
  })

  // Criterio 2: filtrar por voluntario actualiza el calendario
  it('al elegir un voluntario en el filtro, el calendario se vuelve a consultar con ese filtro', async () => {
    const wrapper = await montar([])

    await wrapper.find('select').setValue(2)
    await flushPromises()

    expect(calendarioDisponibilidadService.obtenerCalendario).toHaveBeenLastCalledWith(
      expect.objectContaining({ idVoluntario: 2 })
    )
  })

  // Criterio 2: filtrar por habilidad actualiza el calendario
  it('al elegir una habilidad en el filtro, el calendario se vuelve a consultar con ese filtro', async () => {
    const wrapper = await montar([])

    await wrapper.findAll('select')[1].setValue(3)
    await flushPromises()

    expect(calendarioDisponibilidadService.obtenerCalendario).toHaveBeenLastCalledWith(
      expect.objectContaining({ idHabilidad: 3 })
    )
  })

  // El catálogo de habilidades solo lo puede listar Administrador/Gestor de Tareas (KAN 39-40);
  // si TI no tiene acceso, el filtro de habilidad queda vacío pero el resto de la página funciona
  it('si el catálogo de habilidades falla (p. ej. 403 para TI), el resto de la vista sigue funcionando', async () => {
    voluntarioService.obtenerVoluntarios.mockResolvedValue(VOLUNTARIOS)
    habilidadService.obtenerHabilidades.mockRejectedValue(new Error('Acceso denegado'))
    calendarioDisponibilidadService.obtenerCalendario.mockResolvedValue({ franjas: [] })

    const wrapper = mount(CalendarioDisponibilidadView)
    await flushPromises()

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.findAll('select')[1].findAll('option')).toHaveLength(1)
  })

  it('muestra el error devuelto por el backend', async () => {
    voluntarioService.obtenerVoluntarios.mockResolvedValue(VOLUNTARIOS)
    habilidadService.obtenerHabilidades.mockResolvedValue(HABILIDADES)
    calendarioDisponibilidadService.obtenerCalendario.mockRejectedValue(new Error('El rango del calendario no puede superar los 60 días'))

    const wrapper = mount(CalendarioDisponibilidadView)
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('El rango del calendario no puede superar')
  })
})
