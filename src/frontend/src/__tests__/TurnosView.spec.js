import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}))

vi.mock('../services/turnoService', () => ({
  turnoService: { obtenerTurnos: vi.fn() },
}))

import TurnosView from '../views/TurnosView.vue'
import { turnoService } from '../services/turnoService'

const turno = (extra = {}) => ({
  idTurno: 1, fecha: '2026-10-25', horaInicio: '08:00:00', horaFin: '12:00:00',
  actividad: 'Clasificar alimentos', cuposTotales: 5, cuposOcupados: 2, cuposLibres: 3, ...extra,
})

describe('TurnosView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('muestra horario, actividad y cupos ocupados y libres (criterio 3)', async () => {
    turnoService.obtenerTurnos.mockResolvedValue([turno()])
    const wrapper = mount(TurnosView)
    await flushPromises()

    expect(wrapper.text()).toContain('08:00 – 12:00')
    expect(wrapper.text()).toContain('Clasificar alimentos')
    expect(wrapper.text()).toContain('2 ocupados / 3 libres')
    expect(wrapper.find('.badge-disponible').exists()).toBe(true)
  })

  it('marca como lleno un turno sin cupos libres', async () => {
    turnoService.obtenerTurnos.mockResolvedValue([turno({ cuposOcupados: 5, cuposLibres: 0 })])
    const wrapper = mount(TurnosView)
    await flushPromises()

    expect(wrapper.find('.badge-lleno').exists()).toBe(true)
  })

  it('muestra mensaje vacío', async () => {
    turnoService.obtenerTurnos.mockResolvedValue([])
    const wrapper = mount(TurnosView)
    await flushPromises()

    expect(wrapper.text()).toContain('No hay turnos creados')
  })

  it('muestra el error si la API falla', async () => {
    turnoService.obtenerTurnos.mockRejectedValue(new Error('Error al obtener los turnos'))
    const wrapper = mount(TurnosView)
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('Error al obtener los turnos')
  })

  it('navega a crear turno', async () => {
    turnoService.obtenerTurnos.mockResolvedValue([])
    const wrapper = mount(TurnosView)
    await flushPromises()

    await wrapper.find('.turnos-boton-nuevo').trigger('click')

    expect(push).toHaveBeenCalledWith('/turnos/nuevo')
  })
})
