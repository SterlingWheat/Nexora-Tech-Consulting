import { requestBlob } from './apiClient'

// RF-47 (KAN-58): tipos de reporte soportados y su descripción para la vista
export const TIPOS_REPORTE = [
  { valor: 'voluntarios', etiqueta: 'Voluntarios' },
  { valor: 'tareas', etiqueta: 'Tareas' },
  { valor: 'turnos', etiqueta: 'Turnos' }
]

const descargarBlob = (blob, nombreArchivo) => {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombreArchivo
  document.body.appendChild(enlace)
  enlace.click()
  document.body.removeChild(enlace)
  URL.revokeObjectURL(url)
}

export const reporteService = {
  async exportarPdf(tipo) {
    const blob = await requestBlob(`/reportes/${tipo}/pdf`, {
      mensajeError: 'Error al exportar el reporte a PDF'
    })
    descargarBlob(blob, `${tipo}.pdf`)
  },

  async exportarExcel(tipo) {
    const blob = await requestBlob(`/reportes/${tipo}/excel`, {
      mensajeError: 'Error al exportar el reporte a Excel'
    })
    descargarBlob(blob, `${tipo}.xlsx`)
  }
}
