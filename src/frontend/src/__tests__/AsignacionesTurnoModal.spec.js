import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AsignacionesTurnoModal from '../components/AsignacionesTurnoModal.vue'
import { turnoService } from '../services/turnoService'
import { voluntarioService } from '../services/voluntarioService'

vi.mock('../services/turnoService', () => ({
  turnoService: {
    obtenerAsignaciones: vi.fn(),
    asignarVoluntarios: vi.fn(),
    confirmarAsignacion: vi.fn(),
    cancelarAsignacion: vi.fn(),
  },
}))

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: { obtenerVoluntarios: vi.fn() },
}))

describe('AsignacionesTurnoModal', () => {
  // Para los tests base, usamos una fecha futura (2050) para que el turno se considere "no iniciado"
  const turno = { idTurno: 8, actividad: 'Clasificación', estado: 'ACTIVO', fecha: '2050-01-01', horaInicio: '10:00:00' }
  const asignacion = { idAsignacion: 30, idVoluntario: 1, nombreVoluntario: 'Ana Paz', estado: 'Pendiente' }

  beforeEach(() => {
    vi.clearAllMocks()
    turnoService.obtenerAsignaciones.mockResolvedValue([asignacion])
    turnoService.asignarVoluntarios.mockResolvedValue({ asignaciones: [], advertencias: [] })
    turnoService.confirmarAsignacion.mockResolvedValue(null)
    turnoService.cancelarAsignacion.mockResolvedValue(null)
    voluntarioService.obtenerVoluntarios.mockResolvedValue([
      { idVoluntario: 1, nombre: 'Ana', apellido: 'Paz', activo: true },
      { idVoluntario: 2, nombre: 'Luis', apellido: 'Rojas', activo: true },
      { idVoluntario: 3, nombre: 'Marta', apellido: 'Vega', activo: false },
    ])
  })

  it('muestra historial y solo ofrece voluntarios activos aún no asignados', async () => {
    const wrapper = mount(AsignacionesTurnoModal, {
      props: { visible: false, turno },
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect(wrapper.text()).toContain('Ana Paz')
    expect(wrapper.text()).toContain('Luis Rojas')
    expect(wrapper.text()).not.toContain('Marta Vega')
    expect(wrapper.findAll('.voluntario-opcion')).toHaveLength(1)
  })

  it('asigna voluntarios seleccionados y refresca el historial', async () => {
    const wrapper = mount(AsignacionesTurnoModal, {
      props: { visible: false, turno },
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    await wrapper.find('input[type="checkbox"]').setValue(true)
    await wrapper.find('.btn-guardar').trigger('click')
    await flushPromises()

    expect(turnoService.asignarVoluntarios).toHaveBeenCalledWith(8, [2])
    expect(turnoService.obtenerAsignaciones).toHaveBeenCalledTimes(2)
    expect(wrapper.emitted('updated')).toHaveLength(1)
  })

  // KAN-31 Criterio 3
  it('muestra la advertencia de disponibilidad devuelta por el backend', async () => {
    turnoService.asignarVoluntarios.mockResolvedValue({
      asignaciones: [{ idAsignacion: 31, idVoluntario: 2, nombreVoluntario: 'Luis Rojas', estado: 'Pendiente' }],
      advertencias: ['El voluntario Luis Rojas no tiene disponibilidad declarada para el horario de este turno.'],
    })
    const wrapper = mount(AsignacionesTurnoModal, {
      props: { visible: false, turno },
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    await wrapper.find('input[type="checkbox"]').setValue(true)
    await wrapper.find('.btn-guardar').trigger('click')
    await flushPromises()

    expect(wrapper.find('.modal-aviso').text()).toContain('no tiene disponibilidad declarada')
  })

  // HU2 Criterio 1: Confirmar asignación
  it('confirma una asignación pendiente y refresca el historial', async () => {
    const wrapper = mount(AsignacionesTurnoModal, {
      props: { visible: true, turno },
    })
    await flushPromises()

    await wrapper.find('.btn-confirmar-asignacion').trigger('click')
    await flushPromises()

    expect(turnoService.confirmarAsignacion).toHaveBeenCalledWith(8, 30)
    expect(turnoService.obtenerAsignaciones).toHaveBeenCalledTimes(2)
    expect(wrapper.emitted('updated')).toHaveLength(1)
  })

  it('cancela una asignación vigente antes de que inicie el turno', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = mount(AsignacionesTurnoModal, {
      props: { visible: false, turno },
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()

    await wrapper.find('.btn-cancelar-asignacion').trigger('click')
    await flushPromises()

    expect(turnoService.cancelarAsignacion).toHaveBeenCalledWith(8, 30)
    expect(wrapper.emitted('updated')).toHaveLength(1)
    vi.restoreAllMocks()
  })

  // HU2 Criterio 3: El sistema impide cancelar un turno ya iniciado
  it('inhabilita el botón de cancelar si el turno ya ha iniciado', async () => {
    // Definimos una fecha del pasado
    const turnoPasado = { idTurno: 8, actividad: 'X', estado: 'ACTIVO', fecha: '2000-01-01', horaInicio: '10:00:00' }
    
    const wrapper = mount(AsignacionesTurnoModal, {
      props: { visible: true, turno: turnoPasado },
    })
    await flushPromises()

    const btnCancelar = wrapper.find('.btn-cancelar-asignacion')
    expect(btnCancelar.exists()).toBe(true)
    expect(btnCancelar.element.disabled).toBe(true)
    expect(btnCancelar.attributes('title')).toContain('ya inició')
    
    // Y verificamos que si se le diera clic de alguna forma forzada, la UI muestre el error pre-bloqueo
    await btnCancelar.trigger('click') // En un form normal disabled bloquea, en test-utils a veces emite
    // Pero la función cancelar() del setup valida si turnoIniciado es true.
    expect(turnoService.cancelarAsignacion).not.toHaveBeenCalled()
  })
})