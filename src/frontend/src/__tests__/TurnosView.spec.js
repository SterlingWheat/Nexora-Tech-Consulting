import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/authStore'
import TurnosView from '../views/TurnosView.vue'
import AsignacionesTurnoModal from '../components/AsignacionesTurnoModal.vue'
import { turnoService } from '../services/turnoService'
import { voluntarioService } from '../services/voluntarioService'

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}))

vi.mock('../services/turnoService', () => ({
  turnoService: {
    obtenerTurnos: vi.fn(),
    cancelarTurno: vi.fn(),
    obtenerAsignaciones: vi.fn(),
    asignarVoluntarios: vi.fn(),
    confirmarAsignacion: vi.fn(),
    cancelarAsignacion: vi.fn(),
  },
}))

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: { obtenerVoluntarios: vi.fn() },
}))

const turno = (extra = {}) => ({
  idTurno: 1, fecha: '2026-10-25', horaInicio: '08:00:00', horaFin: '12:00:00',
  actividad: 'Clasificar alimentos', estado: 'ACTIVO',
  cuposTotales: 5, cuposOcupados: 2, cuposLibres: 3, ...extra,
})

describe('TurnosView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useAuthStore().rol = 'GESTOR_TAREAS'
    vi.clearAllMocks()
    turnoService.obtenerAsignaciones.mockResolvedValue([])
    voluntarioService.obtenerVoluntarios.mockResolvedValue([])
  })

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

  it('navega a editar un turno', async () => {
    turnoService.obtenerTurnos.mockResolvedValue([turno()])
    const wrapper = mount(TurnosView)
    await flushPromises()

    const button = wrapper.findAll('.turnos-acciones button')
      .find(candidate => candidate.text() === 'Editar')
    await button.trigger('click')

    expect(push).toHaveBeenCalledWith('/turnos/1/editar')
  })

  it('cancela el turno mediante el endpoint y actualiza la lista', async () => {
    turnoService.obtenerTurnos.mockResolvedValue([turno()])
    turnoService.cancelarTurno.mockResolvedValue({ estado: 'CANCELADO' })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    
    const wrapper = mount(TurnosView)
    await flushPromises()

    await wrapper.find('.accion-cancelar').trigger('click')
    await flushPromises()

    expect(turnoService.cancelarTurno).toHaveBeenCalledWith(1)
    expect(turnoService.obtenerTurnos).toHaveBeenCalledTimes(2)
    vi.restoreAllMocks()
  })

  it('oculta las acciones de gestión a voluntarios', async () => {
    useAuthStore().rol = 'VOLUNTARIO'
    turnoService.obtenerTurnos.mockResolvedValue([turno()])
    const wrapper = mount(TurnosView)
    await flushPromises()

    expect(wrapper.find('.turnos-boton-nuevo').exists()).toBe(false)
    expect(wrapper.find('.turnos-acciones').exists()).toBe(false)
  })

  // HU2: Al emitir "updated" desde el modal, la vista recarga los datos
  it('recarga los turnos cuando el modal de asignaciones emite el evento updated', async () => {
    turnoService.obtenerTurnos.mockResolvedValue([turno()])
    const wrapper = mount(TurnosView)
    await flushPromises()

    // Reseteamos el mock para contar solo las recargas posteriores a la carga inicial
    turnoService.obtenerTurnos.mockClear()

    const modal = wrapper.findComponent(AsignacionesTurnoModal)
    modal.vm.$emit('updated')
    await flushPromises()

    expect(turnoService.obtenerTurnos).toHaveBeenCalledTimes(1)
  })
})