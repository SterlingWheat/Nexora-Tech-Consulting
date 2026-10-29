import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/authStore'
import TareasView from '../views/TareasView.vue'
import { tareaService } from '../services/tareaService'
import { voluntarioService } from '../services/voluntarioService'
import AsignarVoluntarioModal from '../components/AsignarVoluntarioModal.vue'
import CambiarEstadoModal from '../components/CambiarEstadoModal.vue'
import RegistrarAvanceModal from '../components/RegistrarAvanceModal.vue'

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
  useRoute: () => ({ query: {} })
}))

vi.mock('../services/tareaService', () => ({
  tareaService: {
    obtenerCategorias: vi.fn(),
    filtrarTareas: vi.fn()
  }
}))

vi.mock('../services/voluntarioService', () => ({
  voluntarioService: {
    obtenerVoluntarios: vi.fn()
  }
}))

describe('TareasView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    
    tareaService.obtenerCategorias.mockResolvedValue([{ idCategoria: 1, nombre: 'Logística' }])
    voluntarioService.obtenerVoluntarios.mockResolvedValue([{ idVoluntario: 1, nombre: 'Ana', apellido: 'Perez', activo: true }])
  })

  const mountView = async (rol = 'GESTOR_TAREAS', tareas = []) => {
    const authStore = useAuthStore()
    authStore.rol = rol
    
    tareaService.filtrarTareas.mockResolvedValue({
      content: tareas,
      number: 0,
      totalPages: 1,
      totalElements: tareas.length
    })

    const wrapper = mount(TareasView, {
      global: {
        components: {
          AsignarVoluntarioModal,
          CambiarEstadoModal,
          RegistrarAvanceModal
        }
      }
    })
    await flushPromises()
    return wrapper
  }

  it('carga y muestra la lista de tareas', async () => {
    const wrapper = await mountView('GESTOR_TAREAS', [
      { idTarea: 1, titulo: 'Reparto', descripcion: 'Lote A', estado: 'Pendiente', avance: 0 }
    ])
    
    expect(wrapper.text()).toContain('Reparto')
    expect(wrapper.text()).toContain('Lote A')
    expect(wrapper.text()).toContain('0%')
  })

  it('el botón de avance está deshabilitado si la tarea no está en progreso', async () => {
    const wrapper = await mountView('GESTOR_TAREAS', [
      { idTarea: 1, titulo: 'Reparto', estado: 'Pendiente', avance: 0 }
    ])
    
    const btnAvance = wrapper.find('.btn-avance')
    expect(btnAvance.exists()).toBe(true)
    expect(btnAvance.element.disabled).toBe(true)
  })

  it('el botón de avance está habilitado y abre el modal si la tarea está en progreso', async () => {
    const wrapper = await mountView('GESTOR_TAREAS', [
      { idTarea: 2, titulo: 'Clasificar', estado: 'En progreso', avance: 10 }
    ])
    
    const btnAvance = wrapper.find('.btn-avance')
    expect(btnAvance.exists()).toBe(true)
    expect(btnAvance.element.disabled).toBe(false)
    
    await btnAvance.trigger('click')
    await flushPromises()
    
    const modalAvance = wrapper.findComponent(RegistrarAvanceModal)
    expect(modalAvance.exists()).toBe(true)
    expect(modalAvance.props('visible')).toBe(true)
    expect(modalAvance.props('tarea').idTarea).toBe(2)
  })

  it('los botones de gestión no se muestran si el usuario no tiene permisos', async () => {
    const wrapper = await mountView('VOLUNTARIO', [
      { idTarea: 3, titulo: 'Limpieza', estado: 'En progreso', avance: 20 }
    ])
    
    expect(wrapper.find('.th-acciones').exists()).toBe(false)
    expect(wrapper.find('.btn-asignar').exists()).toBe(false)
    expect(wrapper.find('.btn-estado').exists()).toBe(false)
    expect(wrapper.find('.btn-avance').exists()).toBe(false)
  })

  it('navega a la página de nueva tarea al hacer clic en crear tarea', async () => {
    const wrapper = await mountView('ADMINISTRADOR', [])
    await wrapper.find('.tareas-boton-nuevo').trigger('click')
    expect(push).toHaveBeenCalledWith('/tareas/nueva')
  })
})