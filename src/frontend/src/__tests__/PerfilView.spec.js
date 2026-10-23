import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import PerfilView from '../views/PerfilView.vue'

const { obtenerMiPerfil } = vi.hoisted(() => ({
  obtenerMiPerfil: vi.fn()
}))

vi.mock('../services/perfilService', () => ({
  perfilService: {
    obtenerMiPerfil,
    obtenerPerfil: vi.fn()
  }
}))

describe('PerfilView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the volunteer profile without exposing a password', async () => {
    obtenerMiPerfil.mockResolvedValue({
      idUsuario: 7,
      nombre: 'Ana Voluntaria',
      email: 'ana@example.com',
      rol: 'VOLUNTARIO',
      activo: true,
      fechaCreacion: '2026-01-15T10:00:00',
      password: 'must-not-render'
    })

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/perfil', component: PerfilView }]
    })
    await router.push('/perfil')
    await router.isReady()

    const wrapper = mount(PerfilView, {
      global: { plugins: [createPinia(), router] }
    })
    await flushPromises()

    expect(obtenerMiPerfil).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Ana Voluntaria')
    expect(wrapper.text()).toContain('ana@example.com')
    expect(wrapper.text()).not.toContain('must-not-render')
  })
})
