import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AsignacionesTurnoModal from '../components/AsignacionesTurnoModal.vue'
import { turnoService } from '../services/turnoService'
import { voluntarioService } from '../services/voluntarioService'

vi.mock('../services/turnoService', () => ({
  turnoService: {
    obtenerAsignaciones: vi.fn(),
    asignarVoluntarios: vi.fn(),
    cancelarAsignacion: vi.fn(),
  },
}))

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: { obtenerVoluntarios: vi.fn() },
}))

describe('AsignacionesTurnoModal', () => {
  const turno = { idTurno: 8, actividad: 'Clasificación', estado: 'ACTIVO' }
  const asignacion = { idAsignacion: 30, idVoluntario: 1, nombreVoluntario: 'Ana Paz', estado: 'Pendiente' }

  beforeEach(() => {
    vi.clearAllMocks()
    turnoService.obtenerAsignaciones.mockResolvedValue([asignacion])
    turnoService.asignarVoluntarios.mockResolvedValue({ asignaciones: [], advertencias: [] })
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

  // KAN-31 Criterio 3: la advertencia de disponibilidad del backend se muestra sin bloquear la asignación
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

  it('cancela una asignación vigente sin eliminar su fila histórica', async () => {
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
})
