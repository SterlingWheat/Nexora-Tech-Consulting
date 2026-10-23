import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import App from '../App.vue'

describe('App', () => {
  it.each([
    ['ADMINISTRADOR', true, true],
    ['GESTOR_TAREAS', true, false],
    ['TI', false, false],
    ['VOLUNTARIO', false, false]
  ])('shows navigation for %s according to role', async (rol, canManageTasks, canManageUsers) => {
    const pinia = createPinia()
    const authStore = useAuthStore(pinia)
    authStore.token = 'signed-token'
    authStore.rol = rol

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
    })
    await router.push('/tareas')
    await router.isReady()

    const wrapper = mount(App, { global: { plugins: [pinia, router] } })

    expect(wrapper.text()).toContain('Casa Amandita')
    expect(wrapper.find('a[href="/tareas"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/perfil"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/tareas/nueva"]').exists()).toBe(canManageTasks)
    expect(wrapper.find('a[href="/usuarios"]').exists()).toBe(canManageUsers)
  })
})
