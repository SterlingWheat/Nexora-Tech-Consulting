import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

const push = vi.fn()
const route = { params: {} }

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => route,
}))

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: {
    registrarVoluntario: vi.fn(),
    actualizarVoluntario: vi.fn(),
    obtenerVoluntario: vi.fn(),
  },
}))

import VoluntarioFormView from '../views/VoluntarioFormView.vue'
import { voluntarioService } from '../services/voluntarioService'

async function llenar(wrapper, valores) {
  for (const [campo, valor] of Object.entries(valores)) {
    await wrapper.find(`#${campo}`).setValue(valor)
  }
}

const validos = { nombre: 'Ana', apellido: 'Quispe', documento: '123', email: 'ana@correo.com', telefono: '70012345' }

describe('VoluntarioFormView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    route.params = {}
  })

  it('con campos obligatorios completos registra y vuelve a la lista (criterio 1)', async () => {
    voluntarioService.registrarVoluntario.mockResolvedValue({ idVoluntario: 1 })
    const wrapper = mount(VoluntarioFormView)

    await llenar(wrapper, validos)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // El componente agrega la propiedad crearCuenta en falso por defecto al registrar
    const payloadEsperado = { ...validos, crearCuenta: false }
    expect(voluntarioService.registrarVoluntario).toHaveBeenCalledWith(payloadEsperado)
    expect(push).toHaveBeenCalledWith('/voluntarios')
  })

  it('muestra el mensaje del backend si el documento o correo está duplicado (criterio 2)', async () => {
    voluntarioService.registrarVoluntario.mockRejectedValue(new Error('Ya existe un voluntario registrado con el documento 123'))
    const wrapper = mount(VoluntarioFormView)

    await llenar(wrapper, validos)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('Ya existe un voluntario')
    expect(push).not.toHaveBeenCalled()
  })

  it('no llama a la API si falta un campo obligatorio (criterio 4)', async () => {
    const wrapper = mount(VoluntarioFormView)
    await llenar(wrapper, { ...validos, nombre: '   ' })
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('El nombre es obligatorio')
    expect(voluntarioService.registrarVoluntario).not.toHaveBeenCalled()
  })

  it('rechaza un correo con formato inválido (criterio 4)', async () => {
    const wrapper = mount(VoluntarioFormView)
    await llenar(wrapper, { ...validos, email: 'no-es-correo' })
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('formato válido')
    expect(voluntarioService.registrarVoluntario).not.toHaveBeenCalled()
  })

  it('rechaza un teléfono inválido pero acepta el teléfono vacío (criterio 4)', async () => {
    voluntarioService.registrarVoluntario.mockResolvedValue({})
    const wrapper = mount(VoluntarioFormView)

    await llenar(wrapper, { ...validos, telefono: 'abc' })
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('teléfono')
    expect(voluntarioService.registrarVoluntario).not.toHaveBeenCalled()

    await wrapper.find('#telefono').setValue('')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(voluntarioService.registrarVoluntario).toHaveBeenCalledTimes(1)
  })

  it('limita el teléfono a 20 caracteres', () => {
    const wrapper = mount(VoluntarioFormView)
    expect(wrapper.find('#telefono').attributes('maxlength')).toBe('20')
  })

  it('en edición carga los datos y actualiza en lugar de registrar (criterio 3)', async () => {
    route.params = { id: '5' }
    voluntarioService.obtenerVoluntario.mockResolvedValue({ ...validos, idVoluntario: 5 })
    voluntarioService.actualizarVoluntario.mockResolvedValue({})

    const wrapper = mount(VoluntarioFormView)
    await flushPromises()

    expect(wrapper.find('#nombre').element.value).toBe('Ana')
    await wrapper.find('#nombre').setValue('Ana María')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(voluntarioService.actualizarVoluntario).toHaveBeenCalledWith('5', { ...validos, nombre: 'Ana María' })
    expect(voluntarioService.registrarVoluntario).not.toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith('/voluntarios')
  })
})