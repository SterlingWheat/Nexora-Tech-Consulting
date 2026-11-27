import { describe, it, expect, vi, afterEach } from 'vitest'
import { dashboardService } from '../services/dashboardService'

const respuesta = (ok, cuerpo, texto = cuerpo ? JSON.stringify(cuerpo) : '') => ({
  ok, json: async () => cuerpo, text: async () => texto
})

afterEach(() => vi.unstubAllGlobals())

describe('dashboardService', () => {
  it('obtenerMetricas consulta el endpoint de métricas del panel', async () => {
    const metricas = {
      voluntariosActivos: 10, voluntariosInactivos: 2, tareasAbiertas: 5,
      tareasPorEstado: [{ estado: 'Pendiente', cantidad: 3 }],
      turnosProximos: 4, asignacionesPendientes: 6,
    }
    const fetchMock = vi.fn().mockResolvedValue(respuesta(true, metricas))
    vi.stubGlobal('fetch', fetchMock)

    const resultado = await dashboardService.obtenerMetricas()

    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/dashboard\/metricas$/)
    expect(resultado).toEqual(metricas)
  })

  it('propaga el mensaje de error del backend (p. ej. acceso denegado)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(false, null, 'Acceso denegado')))

    await expect(dashboardService.obtenerMetricas()).rejects.toThrow('Acceso denegado')
  })
})
