import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AsignarVoluntarioModal from '../components/AsignarVoluntarioModal.vue'
import { voluntarioService } from '../services/voluntarioService'
import { asignacionTareaService } from '../services/asignacionTareaService'

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: { obtenerVoluntarios: vi.fn() }
}))

vi.mock('../services/asignacionTareaService', () => ({
  asignacionTareaService: {
    asignarVoluntarios: vi.fn(),
    obtenerAsignaciones: vi.fn(),
    cancelarAsignacion: vi.fn(),
    reemplazarVoluntario: vi.fn()
  }
}))

describe('AsignarVoluntarioModal', () => {
  const listaVoluntarios = [
    { idVoluntario: 1, nombre: 'Ana', apellido: 'Quispe', documento: '123', activo: true },
    { idVoluntario: 2, nombre: 'Carlos', apellido: 'Perez', documento: '456', activo: false },
    { idVoluntario: 3, nombre: 'Maria', apellido: 'Gomez', documento: '789', activo: true }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    voluntarioService.obtenerVoluntarios.mockResolvedValue(listaVoluntarios)
    asignacionTareaService.obtenerAsignaciones.mockResolvedValue([])
  })

  afterEach(() => vi.restoreAllMocks())

  it('filtra y muestra únicamente a los voluntarios activos (HU2 Criterio 2)', async () => {
    const wrapper = mount(AsignarVoluntarioModal, {
      props: { visible: false, tarea: { idTarea: 10, titulo: 'Reparto' } }
    })
    
    await wrapper.setProps({ visible: true })
    await flushPromises()
    
    expect(wrapper.text()).toContain('Ana Quispe')
    expect(wrapper.text()).not.toContain('Carlos Perez')
  })

  it('el botón de guardar permanece deshabilitado hasta seleccionar al menos uno', async () => {
    const wrapper = mount(AsignarVoluntarioModal, {
      props: { visible: false, tarea: { idTarea: 10, titulo: 'Reparto' } }
    })
    
    await wrapper.setProps({ visible: true })
    await flushPromises()
    
    const btnGuardar = wrapper.find('.btn-guardar')
    expect(btnGuardar.element.disabled).toBe(true)
    
    const checkbox = wrapper.find('input[type="checkbox"]')
    await checkbox.setValue(true)
    expect(btnGuardar.element.disabled).toBe(false)
  })

  it('asigna los voluntarios seleccionados y emite evento asignado (HU2 Criterio 1)', async () => {
    asignacionTareaService.asignarVoluntarios.mockResolvedValue([])
    
    const wrapper = mount(AsignarVoluntarioModal, {
      props: { visible: false, tarea: { idTarea: 10, titulo: 'Reparto' } }
    })
    
    await wrapper.setProps({ visible: true })
    await flushPromises()
    
    await wrapper.findAll('input[type="checkbox"]')[0].setValue(true) // ID 1
    await wrapper.find('.btn-guardar').trigger('click')
    
    await flushPromises()
    
    expect(asignacionTareaService.asignarVoluntarios).toHaveBeenCalledWith(10, [1])
    expect(wrapper.emitted()).toHaveProperty('asignado')
    expect(wrapper.emitted()).toHaveProperty('close')
  })

  it('cancela una asignación y conserva su fila histórica', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    
    const activa = {
      idAsignacion: 30,
      nombreVoluntario: 'Ana Quispe',
      estadoRespuesta: 'Asignado'
    }
    const cancelada = { ...activa, estadoRespuesta: 'Cancelada' }
    
    asignacionTareaService.obtenerAsignaciones
      .mockResolvedValueOnce([activa])
      .mockResolvedValueOnce([cancelada])
      
    asignacionTareaService.cancelarAsignacion.mockResolvedValue()

    const wrapper = mount(AsignarVoluntarioModal, {
      props: { visible: false, tarea: { idTarea: 10, titulo: 'Reparto' } }
    })
    
    await wrapper.setProps({ visible: true })
    await flushPromises()
    
    await wrapper.find('.btn-cancelar-asignacion').trigger('click')
    await flushPromises()
    
    expect(asignacionTareaService.cancelarAsignacion).toHaveBeenCalledWith(10, 30)
    expect(wrapper.text()).toContain('Cancelada')
    expect(wrapper.find('.btn-cancelar-asignacion').exists()).toBe(false)
    expect(wrapper.emitted()).toHaveProperty('asignado')
  })

  it('reemplaza un voluntario asignado activando el modo reemplazo', async () => {
    const activa = {
      idAsignacion: 40,
      idVoluntario: 1,
      nombreVoluntario: 'Ana Quispe',
      estadoRespuesta: 'Asignado'
    }
    
    asignacionTareaService.obtenerAsignaciones.mockResolvedValue([activa])
    asignacionTareaService.reemplazarVoluntario.mockResolvedValue({})

    const wrapper = mount(AsignarVoluntarioModal, {
      props: { visible: false, tarea: { idTarea: 10, titulo: 'Reparto' } }
    })
    
    await wrapper.setProps({ visible: true })
    await flushPromises()

    // Hacer clic en Reemplazar
    await wrapper.find('.btn-reemplazar').trigger('click')
    
    // Verificamos que se muestre el banner de reemplazo
    expect(wrapper.text()).toContain('Selecciona un voluntario activo para reemplazar a Ana Quispe')

    // Seleccionamos al segundo voluntario de la lista (María Gomez -> ID 3)
    await wrapper.findAll('input[type="checkbox"]')[1].setValue(true)
    
    // Hacemos clic en Confirmar Reemplazo
    await wrapper.find('.btn-confirmar-reemplazo').trigger('click')
    await flushPromises()

    expect(asignacionTareaService.reemplazarVoluntario).toHaveBeenCalledWith(10, 40, 3)
    expect(wrapper.emitted()).toHaveProperty('asignado')
  })
})