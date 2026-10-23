import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import LoginView from '../views/LoginView.vue'

describe('LoginView', () => {
  it('requires an email and password before submission', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/login', component: LoginView }]
    })
    await router.push('/login')
    await router.isReady()

    const wrapper = mount(LoginView, {
      global: { plugins: [createPinia(), router] }
    })

    expect(wrapper.get('input[name="email"]').element.required).toBe(true)
    expect(wrapper.get('input[name="email"]').attributes('type')).toBe('email')
    expect(wrapper.get('input[name="password"]').element.required).toBe(true)
    expect(wrapper.get('input[name="password"]').attributes('type')).toBe('password')
  })
})
