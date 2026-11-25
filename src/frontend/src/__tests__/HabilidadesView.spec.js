import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import HabilidadesView from '../views/HabilidadesView.vue'
import { habilidadService } from '../services/habilidadService'

// Mock del servicio
vi.mock('../services/habilidadService', () => ({
  habilidadService: {
    obtenerHabilidades: vi.fn(),
    crearHabilidad: vi.fn(),
    actualizarHabilidad: vi.fn(),
    eliminarHabilidad: vi.fn()
  }
}))

describe('HabilidadesView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn().mockReturnValue(true)) // Mock para la confirmación de eliminación
  })

  it('renderiza la lista de habilidades al cargar', async () => {
    habilidadService.obtenerHabilidades.mockResolvedValue([
      { id: 1, nombre: 'Liderazgo', descripcion: 'Gestión de equipos' },
      { id: 2, nombre: 'Primeros Auxilios', descripcion: null }
    ])

    const wrapper = mount(HabilidadesView)
    await flushPromises()

    expect(habilidadService.obtenerHabilidades).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Liderazgo')
    expect(wrapper.text()).toContain('Gestión de equipos')
    expect(wrapper.text()).toContain('Primeros Auxilios')
    expect(wrapper.text()).toContain('Sin descripción') // Manejo de nulos
  })

  it('muestra un mensaje cuando el catálogo está vacío', async () => {
    habilidadService.obtenerHabilidades.mockResolvedValue([])

    const wrapper = mount(HabilidadesView)
    await flushPromises()

    expect(wrapper.text()).toContain('No hay habilidades registradas en el catálogo.')
  })

  it('abre el modal de creación y permite guardar una nueva habilidad', async () => {
    habilidadService.obtenerHabilidades.mockResolvedValue([])
    habilidadService.crearHabilidad.mockResolvedValue({ id: 3, nombre: 'Cocina' })

    const wrapper = mount(HabilidadesView)
    await flushPromises()

    // Abrir modal
    await wrapper.find('.vols-boton-nuevo').trigger('click')
    expect(wrapper.find('.modal-contenido h2').text()).toBe('Nueva Habilidad')

    // Llenar formulario
    await wrapper.find('#nombre').setValue('Cocina')
    await wrapper.find('#descripcion').setValue('Preparación de raciones')
    
    // Guardar
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(habilidadService.crearHabilidad).toHaveBeenCalledWith({
      nombre: 'Cocina',
      descripcion: 'Preparación de raciones'
    })
    // Se recarga la tabla
    expect(habilidadService.obtenerHabilidades).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.modal-contenido').exists()).toBe(false)
  })

  it('abre el modal de edición con los datos cargados y permite guardar cambios', async () => {
    habilidadService.obtenerHabilidades.mockResolvedValue([
      { id: 1, nombre: 'Liderazgo', descripcion: 'Gestión de equipos' }
    ])
    habilidadService.actualizarHabilidad.mockResolvedValue({})

    const wrapper = mount(HabilidadesView)
    await flushPromises()

    // Click en Editar (primer botón en acciones-celda)
    await wrapper.find('.vols-boton-editar').trigger('click')
    expect(wrapper.find('.modal-contenido h2').text()).toBe('Editar Habilidad')
    
    // Verifica que los inputs estén poblados
    expect(wrapper.find('#nombre').element.value).toBe('Liderazgo')

    // Cambiar datos
    await wrapper.find('#nombre').setValue('Liderazgo Avanzado')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(habilidadService.actualizarHabilidad).toHaveBeenCalledWith(1, {
      nombre: 'Liderazgo Avanzado',
      descripcion: 'Gestión de equipos'
    })
  })

  it('no permite guardar si el nombre está vacío', async () => {
    habilidadService.obtenerHabilidades.mockResolvedValue([])

    const wrapper = mount(HabilidadesView)
    await flushPromises()

    await wrapper.find('.vols-boton-nuevo').trigger('click')
    
    // Nombre vacío
    await wrapper.find('#nombre').setValue('   ')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('El nombre de la habilidad es obligatorio.')
    expect(habilidadService.crearHabilidad).not.toHaveBeenCalled()
  })

  it('elimina una habilidad tras confirmar', async () => {
    habilidadService.obtenerHabilidades.mockResolvedValue([
      { id: 4, nombre: 'Logística', descripcion: '' }
    ])
    habilidadService.eliminarHabilidad.mockResolvedValue({})

    const wrapper = mount(HabilidadesView)
    await flushPromises()

    // Click en Eliminar (botón con clase btn-desactivar en la celda de acciones)
    await wrapper.find('.btn-desactivar').trigger('click')
    await flushPromises()

    expect(habilidadService.eliminarHabilidad).toHaveBeenCalledWith(4)
    expect(habilidadService.obtenerHabilidades).toHaveBeenCalledTimes(2)
  })
})