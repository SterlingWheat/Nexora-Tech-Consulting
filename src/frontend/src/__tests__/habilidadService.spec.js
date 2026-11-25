import { describe, it, expect, vi, afterEach } from 'vitest'
import { habilidadService } from '../services/habilidadService'

// Helper para mockear fetch tal como se hace en voluntarioService.spec.js
const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

const datosHabilidad = { nombre: 'Liderazgo', descripcion: 'Gestión en campo' }

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('habilidadService', () => {
  it('obtenerHabilidades envía GET y devuelve la lista', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, [{ id: 1, nombre: 'Liderazgo' }]))
    vi.stubGlobal('fetch', fetchMock)
    
    const habilidades = await habilidadService.obtenerHabilidades()
    
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/admin\/habilidades$/)
    expect(opciones.method).toBe('GET') // Es por defecto en apiClient, suele ser undefined o GET
    expect(habilidades).toHaveLength(1)
  })

  it('obtenerHabilidad envía GET con el ID correspondiente', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { id: 5, nombre: 'Cocina' }))
    vi.stubGlobal('fetch', fetchMock)
    
    const habilidad = await habilidadService.obtenerHabilidad(5)
    
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/admin\/habilidades\/5$/)
    expect(habilidad.id).toBe(5)
  })

  it('crearHabilidad envía POST con JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { id: 2, ...datosHabilidad }))
    vi.stubGlobal('fetch', fetchMock)
    
    const creada = await habilidadService.crearHabilidad(datosHabilidad)
    
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/admin\/habilidades$/)
    expect(opciones.method).toBe('POST')
    expect(JSON.parse(opciones.body)).toEqual(datosHabilidad)
    expect(creada.id).toBe(2)
  })

  it('crearHabilidad propaga el error (ej. duplicado)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, 'Ya existe otra habilidad con el nombre: Liderazgo')))
    
    await expect(habilidadService.crearHabilidad(datosHabilidad))
      .rejects.toThrow('Ya existe otra habilidad con el nombre: Liderazgo')
  })

  it('actualizarHabilidad envía PUT con el ID', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { id: 3, ...datosHabilidad }))
    vi.stubGlobal('fetch', fetchMock)
    
    await habilidadService.actualizarHabilidad(3, datosHabilidad)
    
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/admin\/habilidades\/3$/)
    expect(opciones.method).toBe('PUT')
    expect(JSON.parse(opciones.body)).toEqual(datosHabilidad)
  })

  it('eliminarHabilidad envía DELETE al ID indicado', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, {}))
    vi.stubGlobal('fetch', fetchMock)
    
    await habilidadService.eliminarHabilidad(15)
    
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/admin\/habilidades\/15$/)
    expect(opciones.method).toBe('DELETE')
  })
})