import { request } from './apiClient'

export const notificationTemplateService = {
  listar() {
    return request('/admin/email-templates', {
      mensajeError: 'No se pudieron cargar las plantillas'
    })
  },

  actualizar(tipo, template) {
    return request(`/admin/email-templates/${encodeURIComponent(tipo)}`, {
      method: 'PUT',
      body: template,
      mensajeError: 'No se pudo guardar la plantilla'
    })
  },

  enviar(tipo, email) {
    return request(`/admin/email-templates/${encodeURIComponent(tipo)}/send`, {
      method: 'POST',
      body: email,
      mensajeError: 'No se pudo enviar el correo'
    })
  }
}
