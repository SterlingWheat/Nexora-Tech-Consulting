import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest'
import { usuarioService } from '../services/usuarioService'
import { authService } from '../services/authService'

const respuesta = (ok, cuerpo, texto = '') => ({
  ok,
  status: ok ? 200 : 400,
  json: async () => cuerpo,
  text: async () => texto || JSON.stringify(cuerpo)
})

describe('usuarioService', () => {
  beforeEach(() => {
    // Simulamos que el usuario está autenticado para que añada las cabeceras
    vi.spyOn(authService, 'getAuthorizationHeader').mockReturnValue({ Authorization: 'Bearer token-de-prueba' })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('listar envía GET con parámetros de consulta correctos', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, [{ idUsuario: 1, nombre: 'Admin' }]))
    vi.stubGlobal('fetch', fetchMock)

    await usuarioService.listar({ rol: 'ADMINISTRADOR', activo: 'true' })
    const [url, opciones] = fetchMock.mock.calls[0]
    
    expect(url).toContain('/usuarios?rol=ADMINISTRADOR&activo=true')
    expect(opciones.method).toBe('GET')
    expect(opciones.headers.Authorization).toBe('Bearer token-de-prueba')
  })

  it('listar no incluye parámetros vacíos en la URL', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, []))
    vi.stubGlobal('fetch', fetchMock)

    await usuarioService.listar({ rol: '', activo: '' })
    const [url] = fetchMock.mock.calls[0]
    
    expect(url).not.toContain('?')
  })

  it('crear envía POST con el usuario en el cuerpo y JSON Content-Type', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idUsuario: 2 }))
    vi.stubGlobal('fetch', fetchMock)
    
    const datos = { nombre: 'Nuevo Usuario', email: 'nuevo@ejemplo.com', rol: 'VOLUNTARIO' }
    await usuarioService.crear(datos)
    
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/usuarios$/)
    expect(opciones.method).toBe('POST')
    expect(opciones.headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(opciones.body)).toEqual(datos)
  })

  it('actualizar envía PUT con el ID en la URL', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, { idUsuario: 3 }))
    vi.stubGlobal('fetch', fetchMock)
    
    const datos = { nombre: 'Nombre Editado' }
    await usuarioService.actualizar(3, datos)
    
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/usuarios\/3$/)
    expect(opciones.method).toBe('PUT')
    expect(JSON.parse(opciones.body)).toEqual(datos)
  })

  it('desactivar envía PUT al endpoint de desactivación', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, 'Usuario desactivado correctamente.'))
    vi.stubGlobal('fetch', fetchMock)
    
    await usuarioService.desactivar(4)
    const [url, opciones] = fetchMock.mock.calls[0]
    
    expect(url).toMatch(/\/usuarios\/4\/desactivar$/)
    expect(opciones.method).toBe('PUT')
  })

  it('activar envía PUT al endpoint de activación', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, 'Usuario activado correctamente.'))
    vi.stubGlobal('fetch', fetchMock)
    
    await usuarioService.activar(5)
    const [url, opciones] = fetchMock.mock.calls[0]
    
    expect(url).toMatch(/\/usuarios\/5\/activar$/)
    expect(opciones.method).toBe('PUT')
  })

  it('lanza un error con el mensaje del backend cuando la petición falla', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(false, null, '{"message":"El correo ya está registrado."}'))
    vi.stubGlobal('fetch', fetchMock)
    
    await expect(usuarioService.crear({})).rejects.toThrow('El correo ya está registrado.')
  })

  it('usa un mensaje de error por defecto si el backend no responde con un mensaje claro', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(false, null, ''))
    vi.stubGlobal('fetch', fetchMock)
    
    await expect(usuarioService.crear({})).rejects.toThrow('No se pudo completar la operación de usuario.')
  })
})