import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { tareaService } from '../services/tareaService'
import { authService } from '../services/authService'

const mockRespuesta = (ok, datos, texto = '') => ({
  ok,
  status: ok ? 200 : 400,
  json: async () => datos,
  text: async () => texto || JSON.stringify(datos)
})

describe('tareaService', () => {
  beforeEach(() => {
    vi.spyOn(authService, 'getAuthorizationHeader').mockReturnValue({
      Authorization: 'Bearer token-de-prueba'
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('crearTarea envía un POST con el JSON y la cabecera de autenticación', async () => {
    const fetchMock = vi.fn().mockResolvedValue(mockRespuesta(true, { idTarea: 1, titulo: 'Nueva tarea' }))
    vi.stubGlobal('fetch', fetchMock)

    const payload = { titulo: 'Clasificar víveres', descripcion: 'Lote 1', idCategoria: 2 }
    const resultado = await tareaService.crearTarea(payload)

    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/tareas$/)
    expect(opciones.method).toBe('POST')
    expect(opciones.headers.Authorization).toBe('Bearer token-de-prueba')
    expect(JSON.parse(opciones.body)).toEqual(payload)
    expect(resultado.idTarea).toBe(1)
  })

  it('filtrarTareas construye correctamente los parámetros de consulta (HU1)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(mockRespuesta(true, { content: [], totalElements: 0 }))
    vi.stubGlobal('fetch', fetchMock)

    const filtros = { estado: 'Pendiente', idCategoria: 3 }
    await tareaService.filtrarTareas(filtros, 1, 10)

    const [url] = fetchMock.mock.calls[0]
    expect(url).toContain('/tareas/filtrar?')
    expect(url).toContain('page=1')
    expect(url).toContain('size=10')
    expect(url).toContain('estado=Pendiente')
    expect(url).toContain('idCategoria=3')
  })

  it('cambiarEstado envía un PATCH con el nuevo estado (HU3)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(mockRespuesta(true, { idTarea: 5, estado: 'En progreso' }))
    vi.stubGlobal('fetch', fetchMock)

    await tareaService.cambiarEstado(5, 'En progreso')

    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/tareas\/5\/estado$/)
    expect(opciones.method).toBe('PATCH')
    expect(JSON.parse(opciones.body)).toEqual({ nuevoEstado: 'En progreso' })
  })

  it('lanza un error claro cuando el backend responde con error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(mockRespuesta(false, null, 'Transición no permitida')))

    await expect(tareaService.cambiarEstado(1, 'Pendiente')).rejects.toThrow('Transición no permitida')
  })
})