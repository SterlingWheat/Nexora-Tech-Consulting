import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}))

vi.mock('../services/dashboardService', () => ({
  dashboardService: { obtenerMetricas: vi.fn() },
}))

import DashboardView from '../views/DashboardView.vue'
import { dashboardService } from '../services/dashboardService'

const METRICAS = {
  voluntariosActivos: 12,
  voluntariosInactivos: 3,
  tareasAbiertas: 7,
  tareasPorEstado: [
    { estado: 'Pendiente', cantidad: 4 },
    { estado: 'Finalizada', cantidad: 10 },
  ],
  turnosProximos: 5,
  asignacionesPendientes: 9,
}

async function montar(metricas = METRICAS) {
  dashboardService.obtenerMetricas.mockResolvedValue(metricas)
  const wrapper = mount(DashboardView)
  await flushPromises()
  return wrapper
}

describe('DashboardView', () => {
  beforeEach(() => vi.clearAllMocks())

  // Criterio 1: muestra los 4 indicadores clave
  it('muestra los cuatro indicadores clave', async () => {
    const wrapper = await montar()

    expect(wrapper.text()).toContain('12')
    expect(wrapper.text()).toContain('Voluntarios activos')
    expect(wrapper.text()).toContain('7')
    expect(wrapper.text()).toContain('Tareas abiertas')
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('Turnos próximos')
    expect(wrapper.text()).toContain('9')
    expect(wrapper.text()).toContain('Asignaciones pendientes')
  })

  // Criterio 2: cada indicador navega al listado filtrado correspondiente
  it('al hacer clic en "Voluntarios activos", navega a /voluntarios filtrado', async () => {
    const wrapper = await montar()
    await wrapper.findAll('.dash-kpi')[0].trigger('click')

    expect(push).toHaveBeenCalledWith({ path: '/voluntarios', query: { estado: 'activos' } })
  })

  it('al hacer clic en "Tareas abiertas", navega a /tareas filtrado', async () => {
    const wrapper = await montar()
    await wrapper.findAll('.dash-kpi')[1].trigger('click')

    expect(push).toHaveBeenCalledWith({ path: '/tareas', query: { vista: 'abiertas' } })
  })

  it('al hacer clic en "Turnos próximos", navega al calendario de turnos', async () => {
    const wrapper = await montar()
    await wrapper.findAll('.dash-kpi')[2].trigger('click')

    expect(push).toHaveBeenCalledWith({ path: '/turnos/calendario', query: undefined })
  })

  it('al hacer clic en "Asignaciones pendientes", navega a /tareas', async () => {
    const wrapper = await montar()
    await wrapper.findAll('.dash-kpi')[3].trigger('click')

    expect(push).toHaveBeenCalledWith({ path: '/tareas', query: undefined })
  })

  it('muestra el desglose de tareas por estado para el gráfico', async () => {
    const wrapper = await montar()

    expect(wrapper.text()).toContain('Pendiente')
    expect(wrapper.text()).toContain('Finalizada')
  })

  it('muestra el error devuelto por el backend', async () => {
    dashboardService.obtenerMetricas.mockRejectedValue(new Error('Acceso denegado'))

    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('Acceso denegado')
  })
})
