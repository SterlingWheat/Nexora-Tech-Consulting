import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import NotificationTemplatesView from '../views/NotificationTemplatesView.vue'

const { listar, actualizar, enviar } = vi.hoisted(() => ({
  listar: vi.fn(),
  actualizar: vi.fn(),
  enviar: vi.fn()
}))

vi.mock('../services/notificationTemplateService', () => ({
  notificationTemplateService: { listar, actualizar, enviar }
}))

const templates = [
  {
    templateType: 'ASIGNACION',
    name: 'Asignación',
    subject: 'Asignada: {{actividad}}',
    htmlBody: '<p>{{nombre}} {{actividad}} {{fecha}}</p>',
    textBody: '{{nombre}} {{actividad}} {{fecha}}',
    active: true
  },
  {
    templateType: 'RECORDATORIO',
    name: 'Recordatorio',
    subject: 'Recordatorio: {{actividad}}',
    htmlBody: '<p>{{nombre}} {{actividad}} {{fecha}}</p>',
    textBody: '{{nombre}} {{actividad}} {{fecha}}',
    active: true
  },
  {
    templateType: 'CAMBIO_TURNO',
    name: 'Cambio de turno',
    subject: 'Turno: {{actividad}}',
    htmlBody: '<p>{{nombre}} {{actividad}} {{fecha}}</p>',
    textBody: '{{nombre}} {{actividad}} {{fecha}}',
    active: true
  }
]

const mountView = async () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/plantillas-notificacion', component: NotificationTemplatesView }]
  })
  await router.push('/plantillas-notificacion')
  await router.isReady()
  const wrapper = mount(NotificationTemplatesView, {
    global: { plugins: [createPinia(), router] }
  })
  await flushPromises()
  return wrapper
}

describe('NotificationTemplatesView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listar.mockResolvedValue(templates.map((template) => ({ ...template })))
    actualizar.mockImplementation(async (type, changes) => ({
      ...templates.find((template) => template.templateType === type),
      ...changes
    }))
    enviar.mockResolvedValue(null)
  })

  it('carga y permite editar y guardar una plantilla persistida', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Asignación')
    await wrapper.get('.plantillas-editor input:not([type="checkbox"])')
      .setValue('Asignada: {{actividad}} - actualizada')
    await wrapper.get('form.plantillas-editor').trigger('submit')
    await flushPromises()

    expect(actualizar).toHaveBeenCalledWith('ASIGNACION', {
      subject: 'Asignada: {{actividad}} - actualizada',
      htmlBody: templates[0].htmlBody,
      textBody: templates[0].textBody,
      active: true
    })
    expect(wrapper.text()).toContain('Los siguientes envíos usarán esta versión.')
  })

  it('envía con la plantilla seleccionada y valores dinámicos proporcionados', async () => {
    const wrapper = await mountView()
    const inputs = wrapper.findAll('form.plantillas-envio input')
    await inputs[0].setValue('ana@example.com')
    await inputs[1].setValue('Ana')
    await inputs[2].setValue('Limpieza')
    await inputs[3].setValue('21/12/2026')
    await wrapper.get('form.plantillas-envio').trigger('submit')
    await flushPromises()

    expect(enviar).toHaveBeenCalledWith('ASIGNACION', {
      recipient: 'ana@example.com',
      name: 'Ana',
      activity: 'Limpieza',
      date: '21/12/2026'
    })
    expect(wrapper.text()).toContain('aceptó el mensaje para envío')
  })
})
