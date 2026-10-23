import { describe, it, expect, vi, afterEach } from 'vitest'
import { voluntarioService } from '../services/voluntarioService'

const respuesta = (ok, cuerpo, texto = '') => ({
  ok,
  json: async () => cuerpo,
  text: async () => texto,
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
})
