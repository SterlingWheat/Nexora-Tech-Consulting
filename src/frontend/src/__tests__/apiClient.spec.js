import { describe, it, expect, vi, afterEach } from 'vitest'
import { request } from '../services/apiClient'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

afterEach(() => vi.unstubAllGlobals())

describe('apiClient.request', () => {
  it('hace GET sin cuerpo ni cabeceras y devuelve el JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { ok: 1 }))
    vi.stubGlobal('fetch', fetchMock)
    expect(await request('/x')).toEqual({ ok: 1 })
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/x$/)
    expect(opciones).toEqual({ method: 'GET', headers: {} }) // Corrección aplicada aquí
  })

  it('serializa el cuerpo como JSON con Content-Type', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, {}))
    vi.stubGlobal('fetch', fetchMock)
    await request('/x', { method: 'PUT', body: { a: 1 } })
    const opciones = fetchMock.mock.calls[0][1]
    expect(opciones.method).toBe('PUT')
    expect(opciones.headers['Content-Type']).toBe('application/json')
    expect(opciones.body).toBe('{"a":1}')
  })

  it('lanza el texto del backend cuando la respuesta no es ok', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, 'Mensaje claro')))
    await expect(request('/x')).rejects.toThrow('Mensaje claro')
  })

  it('usa el mensaje por defecto si el backend no envía texto', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, '')))
    await expect(request('/x', { mensajeError: 'Falló' })).rejects.toThrow('Falló')
  })
})