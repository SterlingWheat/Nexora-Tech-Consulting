import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTareaStore } from '../stores/tareaStore'
import { tareaService } from '../services/tareaService'

vi.mock('../services/tareaService', () => ({
  tareaService: {
    filtrarTareas: vi.fn(),
    cambiarEstado: vi.fn(),
    crearTarea: vi.fn()
  }
}))

describe('tareaStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchTareasFiltradas actualiza la paginación y la lista en el estado (HU1)', async () => {
    const mockRespuesta = {
      content: [{ idTarea: 1, titulo: 'Tarea 1' }],
      number: 0,
      totalPages: 1,
      totalElements: 1
    }
    tareaService.filtrarTareas.mockResolvedValue(mockRespuesta)

    const store = useTareaStore()
    await store.fetchTareasFiltradas()

    expect(store.tareas).toHaveLength(1)
    expect(store.paginaActual).toBe(0)
    expect(store.totalElementos).toBe(1)
    expect(store.cargando).toBe(false)
  })

  it('cambiarEstadoTarea actualiza el estado de la tarea en memoria (HU3)', async () => {
    const store = useTareaStore()
    store.tareas = [{ idTarea: 10, estado: 'Pendiente' }]

    tareaService.cambiarEstado.mockResolvedValue({ idTarea: 10, estado: 'En progreso' })

    const resultado = await store.cambiarEstadoTarea(10, 'En progreso')

    expect(resultado.exito).toBe(true)
    expect(store.tareas[0].estado).toBe('En progreso')
  })

  it('maneja errores en cambiarEstadoTarea y guarda el mensaje', async () => {
    tareaService.cambiarEstado.mockRejectedValue(new Error('No se puede cambiar el estado'))

    const store = useTareaStore()
    const resultado = await store.cambiarEstadoTarea(10, 'Finalizada')

    expect(resultado.exito).toBe(false)
    expect(store.error).toBe('No se puede cambiar el estado')
  })
})