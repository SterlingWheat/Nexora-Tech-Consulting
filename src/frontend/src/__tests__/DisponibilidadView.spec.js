import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => ({ params: { id: '4' } }),
}))

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: { obtenerVoluntario: vi.fn() },
}))

vi.mock('../services/disponibilidadService', async (importOriginal) => {
  const original = await importOriginal()
  return { ...original, disponibilidadService: { obtener: vi.fn(), guardar: vi.fn() } }
})

import DisponibilidadView from '../views/DisponibilidadView.vue'
import { voluntarioService } from '../services/voluntarioService'
import { disponibilidadService } from '../services/disponibilidadService'

async function montar(franjasIniciales = []) {
  voluntarioService.obtenerVoluntario.mockResolvedValue({ nombre: 'Ana', apellido: 'Quispe' })
  disponibilidadService.obtener.mockResolvedValue({ idVoluntario: 4, franjas: franjasIniciales, advertencias: [] })
  const wrapper = mount(DisponibilidadView)
  await flushPromises()
  return wrapper
}

const botonAgregar = (wrapper, i) => wrapper.findAll('.disp-agregar')[i]

describe('DisponibilidadView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('carga y muestra la disponibilidad guardada en el perfil (criterio 1)', async () => {
    const wrapper = await montar([{ diaSemana: 'MARTES', horaInicio: '09:00:00', horaFin: '13:00:00' }])

    expect(wrapper.text()).toContain('Ana Quispe')
    const inputs = wrapper.findAll('input[type="time"]')
    expect(inputs).toHaveLength(2)
    expect(inputs[0].element.value).toBe('09:00')
    expect(inputs[1].element.value).toBe('13:00')
  })

  it('agrega una franja y la guarda con el formato del backend (criterio 1)', async () => {
    const wrapper = await montar()
    disponibilidadService.guardar.mockResolvedValue({ franjas: [{ diaSemana: 'LUNES', horaInicio: '08:00:00', horaFin: '12:00:00' }], advertencias: [] })

    await botonAgregar(wrapper, 0).trigger('click')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(disponibilidadService.guardar).toHaveBeenCalledWith('4', [{ diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '12:00' }])
    expect(wrapper.text()).toContain('Disponibilidad guardada')
  })

  it('no llama a la API si la hora de fin es anterior a la de inicio (criterio 2)', async () => {
    const wrapper = await montar()

    await botonAgregar(wrapper, 0).trigger('click')
    const [inicio, fin] = wrapper.findAll('input[type="time"]')
    await inicio.setValue('15:00')
    await fin.setValue('10:00')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('hora de fin debe ser posterior')
    expect(disponibilidadService.guardar).not.toHaveBeenCalled()
  })

  it('muestra el rechazo del backend (criterio 2)', async () => {
    const wrapper = await montar()
    disponibilidadService.guardar.mockRejectedValue(new Error('La hora de fin debe ser posterior'))

    await botonAgregar(wrapper, 0).trigger('click')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('La hora de fin debe ser posterior')
  })

  it('muestra la advertencia y las franjas unificadas devueltas por el backend (criterio 3)', async () => {
    const wrapper = await montar()
    disponibilidadService.guardar.mockResolvedValue({
      franjas: [{ diaSemana: 'LUNES', horaInicio: '08:00:00', horaFin: '14:00:00' }],
      advertencias: ['Se unificaron franjas superpuestas del LUNES'],
    })

    await botonAgregar(wrapper, 0).trigger('click')
    await botonAgregar(wrapper, 0).trigger('click')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="status"]').text()).toContain('Se unificaron franjas')
    expect(wrapper.findAll('input[type="time"]')).toHaveLength(2)
  })

  it('quita una franja', async () => {
    const wrapper = await montar([{ diaSemana: 'LUNES', horaInicio: '08:00:00', horaFin: '12:00:00' }])

    await wrapper.find('.disp-quitar').trigger('click')

    expect(wrapper.findAll('input[type="time"]')).toHaveLength(0)
    expect(wrapper.text()).toContain('Sin disponibilidad')
  })

  it('muestra el error si el voluntario no existe', async () => {
    voluntarioService.obtenerVoluntario.mockRejectedValue(new Error('Voluntario no encontrado con ID: 4'))
    disponibilidadService.obtener.mockRejectedValue(new Error('Voluntario no encontrado con ID: 4'))
    const wrapper = mount(DisponibilidadView)
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('Voluntario no encontrado')
  })
})
