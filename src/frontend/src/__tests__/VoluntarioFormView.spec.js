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

// HU2: Mock de habilidadService para cargar checkboxes
vi.mock('../services/habilidadService', () => ({
  habilidadService: {
    obtenerHabilidades: vi.fn(),
  }
}))

import VoluntarioFormView from '../views/VoluntarioFormView.vue'
import { voluntarioService } from '../services/voluntarioService'
import { habilidadService } from '../services/habilidadService'

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
    habilidadService.obtenerHabilidades.mockResolvedValue([
      { id: 1, nombre: 'Primeros Auxilios' },
      { id: 2, nombre: 'Manejo defensivo' }
    ])
  })

  it('con campos obligatorios completos registra y vuelve a la lista (incluyendo habilidades vacías por defecto)', async () => {
    voluntarioService.registrarVoluntario.mockResolvedValue({ idVoluntario: 1 })
    const wrapper = mount(VoluntarioFormView)
    await flushPromises() // Para que cargue las habilidades

    await llenar(wrapper, validos)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    // El componente agrega crearCuenta: false y habilidadesIds: [] por defecto
    const payloadEsperado = { ...validos, crearCuenta: false, habilidadesIds: [] }
    expect(voluntarioService.registrarVoluntario).toHaveBeenCalledWith(payloadEsperado)
    expect(push).toHaveBeenCalledWith('/voluntarios')
  })

  // HU2: Criterio 1 - Asignar habilidades en el registro
  it('envía los IDs de habilidades seleccionadas al registrar', async () => {
    voluntarioService.registrarVoluntario.mockResolvedValue({ idVoluntario: 2 })
    const wrapper = mount(VoluntarioFormView)
    await flushPromises()

    await llenar(wrapper, validos)
    
    // Seleccionamos la segunda habilidad del DOM (id 2)
    const checkboxes = wrapper.findAll('.vol-habilidad-item input[type="checkbox"]')
    await checkboxes[1].setValue(true) // ID 2
    
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const payloadEsperado = { ...validos, crearCuenta: false, habilidadesIds: [2] }
    expect(voluntarioService.registrarVoluntario).toHaveBeenCalledWith(payloadEsperado)
  })

  it('muestra el mensaje del backend si el documento o correo está duplicado', async () => {
    voluntarioService.registrarVoluntario.mockRejectedValue(new Error('Ya existe un voluntario registrado con el documento 123'))
    const wrapper = mount(VoluntarioFormView)
    await flushPromises()

    await llenar(wrapper, validos)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('Ya existe un voluntario')
    expect(push).not.toHaveBeenCalled()
  })

  it('no llama a la API si falta un campo obligatorio', async () => {
    const wrapper = mount(VoluntarioFormView)
    await flushPromises()
    
    await llenar(wrapper, { ...validos, nombre: '   ' })
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('El nombre es obligatorio')
    expect(voluntarioService.registrarVoluntario).not.toHaveBeenCalled()
  })

  // HU2: Criterio 1 - Cargar habilidades previas al editar
  it('en edición carga los datos (incluyendo habilidades previas) y actualiza', async () => {
    route.params = { id: '5' }
    // Simulamos que el voluntario ya tiene la habilidad 1 asignada
    voluntarioService.obtenerVoluntario.mockResolvedValue({ 
      ...validos, 
      idVoluntario: 5,
      habilidades: [{ id: 1, nombre: 'Primeros Auxilios' }] 
    })
    voluntarioService.actualizarVoluntario.mockResolvedValue({})

    const wrapper = mount(VoluntarioFormView)
    await flushPromises()

    expect(wrapper.find('#nombre').element.value).toBe('Ana')
    
    // Verificamos que el checkbox de la habilidad 1 esté marcado
    const checkbox1 = wrapper.findAll('.vol-habilidad-item input[type="checkbox"]')[0]
    expect(checkbox1.element.checked).toBe(true)

    // Modificamos el nombre
    await wrapper.find('#nombre').setValue('Ana María')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(voluntarioService.actualizarVoluntario).toHaveBeenCalledWith('5', { 
      ...validos, 
      nombre: 'Ana María',
      habilidadesIds: [1] // Se debió mantener la habilidad 1
    })
    expect(voluntarioService.registrarVoluntario).not.toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith('/voluntarios')
  })
})