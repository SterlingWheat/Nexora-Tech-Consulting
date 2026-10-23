import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}))

vi.mock('../services/turnoService', () => ({
  turnoService: { crearTurno: vi.fn() },
}))

import TurnoFormView from '../views/TurnoFormView.vue'
import { turnoService } from '../services/turnoService'

const manana = () => {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function llenar(wrapper, valores) {
  for (const [campo, valor] of Object.entries(valores)) {
    await wrapper.find(`#${campo}`).setValue(valor)
  }
}

const validos = () => ({ fecha: manana(), horaInicio: '08:00', horaFin: '12:00', actividad: 'Clasificar alimentos', cuposTotales: 5 })

async function enviar(wrapper) {
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('TurnoFormView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('con datos válidos crea el turno y vuelve a la lista (criterio 1)', async () => {
    turnoService.crearTurno.mockResolvedValue({ idTurno: 1 })
    const wrapper = mount(TurnoFormView)
    const datos = validos()

    await llenar(wrapper, datos)
    await enviar(wrapper)

    expect(turnoService.crearTurno).toHaveBeenCalledWith(datos)
    expect(push).toHaveBeenCalledWith('/turnos')
  })

  it('rechaza una fecha pasada sin llamar a la API (criterio 2)', async () => {
    const wrapper = mount(TurnoFormView)

    await llenar(wrapper, { ...validos(), fecha: '2020-01-01' })
    await enviar(wrapper)

    expect(wrapper.find('[role="alert"]').text()).toContain('no puede ser pasada')
    expect(turnoService.crearTurno).not.toHaveBeenCalled()
  })

  it('rechaza hora de fin anterior o igual a la de inicio (criterio 2)', async () => {
    const wrapper = mount(TurnoFormView)

    await llenar(wrapper, { ...validos(), horaInicio: '12:00', horaFin: '08:00' })
    await enviar(wrapper)
    expect(wrapper.find('[role="alert"]').text()).toContain('hora de fin debe ser posterior')

    await wrapper.find('#horaFin').setValue('12:00')
    await enviar(wrapper)
    expect(wrapper.find('[role="alert"]').text()).toContain('hora de fin debe ser posterior')
    expect(turnoService.crearTurno).not.toHaveBeenCalled()
  })

  it('exige actividad y cupos válidos', async () => {
    const wrapper = mount(TurnoFormView)

    await llenar(wrapper, { ...validos(), actividad: '   ' })
    await enviar(wrapper)
    expect(wrapper.find('[role="alert"]').text()).toContain('La actividad es obligatoria')

    await llenar(wrapper, { actividad: 'X', cuposTotales: 0 })
    await enviar(wrapper)
    expect(wrapper.find('[role="alert"]').text()).toContain('al menos 1')
    expect(turnoService.crearTurno).not.toHaveBeenCalled()
  })

  it('muestra el rechazo del backend (criterio 2)', async () => {
    turnoService.crearTurno.mockRejectedValue(new Error('El horario del turno ya terminó'))
    const wrapper = mount(TurnoFormView)

    await llenar(wrapper, validos())
    await enviar(wrapper)

    expect(wrapper.find('[role="alert"]').text()).toContain('El horario del turno ya terminó')
    expect(push).not.toHaveBeenCalled()
  })
})
