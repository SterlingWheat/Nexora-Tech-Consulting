import { describe, it, expect, vi, afterEach } from 'vitest'
import { voluntarioService } from '../services/voluntarioService'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

const datos = { nombre: 'Ana', apellido: 'Quispe', documento: '123', email: 'ana@correo.com', telefono: '' }

afterEach(() => vi.unstubAllGlobals())

describe('voluntarioService', () => {
  it('registrarVoluntario envía POST con JSON y devuelve el voluntario creado', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idVoluntario: 1, activo: true }))
    vi.stubGlobal('fetch', fetchMock)
    const creado = await voluntarioService.registrarVoluntario(datos)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/voluntarios$/)
    expect(opciones.method).toBe('POST')
    expect(JSON.parse(opciones.body)).toEqual(datos)
    expect(creado.activo).toBe(true)
  })

  it('registrarVoluntario propaga el mensaje claro del backend ante duplicado (409)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, 'Ya existe un voluntario registrado con el documento 123')))
    await expect(voluntarioService.registrarVoluntario(datos)).rejects.toThrow('Ya existe un voluntario registrado con el documento 123')
  })

  it('usa un mensaje por defecto si el backend no devuelve texto', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, '')))
    await expect(voluntarioService.registrarVoluntario(datos)).rejects.toThrow('Error al registrar el voluntario')
  })

  it('actualizarVoluntario envía PUT al id indicado', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idVoluntario: 7 }))
    vi.stubGlobal('fetch', fetchMock)
    await voluntarioService.actualizarVoluntario(7, datos)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/voluntarios\/7$/)
    expect(opciones.method).toBe('PUT')
  })

  it('obtenerVoluntario usa /detalle porque /{id} pertenece al perfil de usuario (KAN-16)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idVoluntario: 5 }))
    vi.stubGlobal('fetch', fetchMock)
    await voluntarioService.obtenerVoluntario(5)
    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/voluntarios\/5\/detalle$/)
  })

  it('obtenerVoluntarios devuelve la lista', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(true, [{ idVoluntario: 1 }])))
    expect(await voluntarioService.obtenerVoluntarios()).toHaveLength(1)
  })

  it('activarVoluntario envía PUT para reactivar', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idVoluntario: 5, activo: true }))
    vi.stubGlobal('fetch', fetchMock)
    await voluntarioService.activarVoluntario(5)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/voluntarios\/5\/activar$/)
    expect(opciones.method).toBe('PUT')
  })

  it('desactivarVoluntario envía PUT para desactivar', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idVoluntario: 5, activo: false }))
    vi.stubGlobal('fetch', fetchMock)
    await voluntarioService.desactivarVoluntario(5)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/voluntarios\/5\/desactivar$/)
    expect(opciones.method).toBe('PUT')
  })

  // KAN-27 Criterio 1: búsqueda por texto
  it('buscarVoluntarios envía el texto como parámetro de consulta', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { content: [], totalElements: 0 }))
    vi.stubGlobal('fetch', fetchMock)
    await voluntarioService.buscarVoluntarios({ texto: 'ana' })
    const url = fetchMock.mock.calls[0][0]
    expect(url).toMatch(/\/api\/voluntarios\/filtrar\?/)
    expect(url).toContain('texto=ana')
    expect(url).toContain('page=0')
    expect(url).toContain('size=10')
  })

  // KAN-27 Criterio 2: filtros de estado y disponibilidad
  it('buscarVoluntarios envía activo y diaSemana cuando se especifican', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { content: [], totalElements: 0 }))
    vi.stubGlobal('fetch', fetchMock)
    await voluntarioService.buscarVoluntarios({ activo: false, diaSemana: 'LUNES' })
    const url = fetchMock.mock.calls[0][0]
    expect(url).toContain('activo=false')
    expect(url).toContain('diaSemana=LUNES')
  })

  // KAN-27: sin filtros no deben enviarse parámetros vacíos (texto/activo/diaSemana)
  it('buscarVoluntarios omite texto, activo y diaSemana cuando no se especifican', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { content: [], totalElements: 0 }))
    vi.stubGlobal('fetch', fetchMock)
    await voluntarioService.buscarVoluntarios()
    const url = fetchMock.mock.calls[0][0]
    expect(url).not.toContain('texto=')
    expect(url).not.toContain('activo=')
    expect(url).not.toContain('diaSemana=')
  })

  // KAN-27 Criterio 3: paginación explícita
  it('buscarVoluntarios envía page y size explícitos', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { content: [], totalElements: 0 }))
    vi.stubGlobal('fetch', fetchMock)
    await voluntarioService.buscarVoluntarios({ page: 3, size: 25 })
    const url = fetchMock.mock.calls[0][0]
    expect(url).toContain('page=3')
    expect(url).toContain('size=25')
  })
})